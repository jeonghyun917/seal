package kr.or.arte.seal.statistics.application;

import kr.or.arte.seal.common.web.StatusLabels;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import java.time.*;
import java.util.*;

public final class StatisticsWorkbook {
    private StatisticsWorkbook() {}
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    public static final class Styles {
        public final CellStyle title, header, body, stripe, number, percent, date, dateTime;
        public Styles(Workbook book) {
            body=style(book,11,false,IndexedColors.DARK_BLUE,null);
            stripe=style(book,11,false,IndexedColors.DARK_BLUE,IndexedColors.LIGHT_CORNFLOWER_BLUE);
            title=style(book,16,true,IndexedColors.DARK_BLUE,null);
            header=style(book,11,true,IndexedColors.WHITE,IndexedColors.DARK_BLUE);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setDataFormat(book.createDataFormat().getFormat("#,##0"));
            number=format(book,body,"#,##0"); percent=format(book,body,"0.0%");
            date=format(book,body,"yyyy-mm-dd"); dateTime=format(book,body,"yyyy-mm-dd hh:mm:ss");
            date.setAlignment(HorizontalAlignment.LEFT);dateTime.setAlignment(HorizontalAlignment.LEFT);
        }
        private static CellStyle format(Workbook b,CellStyle base,String f) {
            CellStyle s=b.createCellStyle();s.cloneStyleFrom(base);
            s.setDataFormat(b.createDataFormat().getFormat(f));s.setAlignment(HorizontalAlignment.RIGHT);return s;
        }
        private static CellStyle style(Workbook b,int size,boolean bold,IndexedColors color,IndexedColors fill) {
            Font f=b.createFont();f.setFontName("맑은 고딕");f.setFontHeightInPoints((short)size);
            f.setBold(bold);
            ((XSSFFont)f).setColor(rgb(color==IndexedColors.WHITE?"FFFFFF":"243B53"));
            CellStyle s=b.createCellStyle();s.setFont(f);s.setVerticalAlignment(VerticalAlignment.CENTER);s.setWrapText(true);
            s.setIndention((short)1);
            if(fill!=null){((XSSFCellStyle)s).setFillForegroundColor(rgb(fill==IndexedColors.DARK_BLUE?"17324D":"F0F5FC"));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);}return s;
        }
        private static XSSFColor rgb(String hex) {return new XSSFColor(java.util.HexFormat.of().parseHex(hex),null);}
    }
    public static void heading(Sheet sh,Styles s,String title,LocalDate from,LocalDate to,LocalDateTime at) {
        sh.setDisplayGridlines(false);
        row(sh,1,s.title,title);
        sh.addMergedRegion(new CellRangeAddress(1,1,0,5));
        row(sh,3,s.body,"조회 시작일",from,"종료일",to);
        sh.getRow(3).getCell(1).setCellStyle(s.date);sh.getRow(3).getCell(3).setCellStyle(s.date);
        row(sh,4,s.body,"추출 일시 (한국시간)",at);sh.getRow(4).getCell(1).setCellStyle(s.dateTime);
        row(sh,5,s.body,"범위","로그인 관리자에게 허용된 조직. 종료일 전체 포함. 상태는 추출 시점 기준.");
        sh.addMergedRegion(new CellRangeAddress(5,5,1,5));
        row(sh,6,s.body,"개인정보","이름·이메일은 현재 수신자 정보 기준 마스킹. 암호화 본문·인증 링크·보안 키는 미포함.");
        sh.addMergedRegion(new CellRangeAddress(6,6,1,5));
        for(int i=0;i<6;i++)sh.setColumnWidth(i,26*256);
        sh.getPrintSetup().setLandscape(true);sh.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
        sh.getPrintSetup().setFitWidth((short)1);sh.getPrintSetup().setFitHeight((short)0);sh.setFitToPage(true);
    }
    public static void summary(Workbook book,Styles st,StatisticsSnapshot s,LocalDate from,LocalDate to,LocalDateTime at) {
        Sheet sh=book.createSheet("통합요약");heading(sh,st,"운영 통계 통합요약",from,to,at);
        sh.setColumnWidth(0,25*256);sh.setColumnWidth(1,28*256);sh.setColumnWidth(2,14*256);
        sh.setColumnWidth(3,18*256);sh.setColumnWidth(4,36*256);sh.setColumnWidth(5,64*256);
        String[] domains={"request","response","mail","evidence","rights","retention"};
        String[] titles={"동의 요청","동의 응답·철회","메일","증적","권리요청","파기·보존 보류"};
        String[] codes={"DRAFT READY ACTIVE COMPLETED EXPIRED CANCELLED","SUBMITTED DECLINED_ALL",
            "QUEUED PROCESSING SMTP_ACCEPTED RETRY_SCHEDULED SEND_FAILED CANCELLED DESTROYED",
            "QUEUED PROCESSING FINALIZED FAILED DESTROYED",
            "RECEIVED IDENTITY_VERIFIED IN_REVIEW APPROVED REJECTED COMPLETED CANCELLED",
            "DUE PENDING_APPROVAL APPROVED EXECUTING EXECUTED CONFIRMED FAILED HELD CANCELLED"};
        String[] wc={"","WITHDRAWN","","VERIFY_INVALID_HMAC VERIFY_INVALID_HASH VERIFY_MISSING_FILE","OVERDUE","LEGAL_HOLD_ACTIVE"};
        String[] basis={"요청 생성 일시","응답 제출 일시","발송함 등록 일시 (실제 발송일 아님)","증적 생성 대기 등록 일시","권리요청 접수 등록 일시","파기 작업 요청 일시"};
        String[] warningBasis={"","철회 발생 일시 기준. 응답 제출 기간과 다를 수 있음.","",
            "기간 내 검증 실패 사건 수. 동일 증적 반복 검증 포함.",
            "기간 내 접수 중 현재 기한 초과. 완료·취소 제외.",
            "기간 내 등록 중 현재 활성·미만료 보존 보류."};
        List<List<StatusCount>> groups=List.of(s.requests(),s.responses(),s.mail(),s.evidence(),s.rights(),s.retention());
        List<List<StatusCount>> warnings=List.of(List.of(),s.responseWarnings(),List.of(),s.evidenceWarnings(),s.rightsWarnings(),s.retentionWarnings());
        int line=8;
        for(int g=0;g<6;g++){
            row(sh,line,st.title,titles[g]);
            sh.addMergedRegion(new CellRangeAddress(line,line++,0,5));
            row(sh,line++,st.header,"구분","상태·지표","건수","상태 총계 대비 비율","상태 코드","집계 기준·설명");
            Map<String,Long> counts=counts(codes[g],groups.get(g),g==4);
            int start=line, total=line+counts.size()+1;
            for(var e:counts.entrySet()){
                row(sh,line,st.body,"상태별 현황",StatusLabels.label(domains[g],e.getKey()),e.getValue(),"",e.getKey(),basis[g]);
                sh.getRow(line).getCell(2).setCellStyle(st.number);
                Cell ratio=sh.getRow(line).getCell(3);
                ratio.setCellFormula("IF(C"+total+"=0,\"-\",C"+(line+1)+"/C"+total+")");ratio.setCellStyle(st.percent);line++;
            }
            row(sh,line,st.header,"상태 총계",null,null,null,null,"별도 지표는 합산하지 않음");
            sh.getRow(line).getCell(2).setCellFormula("SUM(C"+(start+1)+":C"+line+")");line++;
            for(var e:counts(wc[g],warnings.get(g),false).entrySet()){
                row(sh,line,st.stripe,"별도 지표 (미합산)",StatusLabels.label(domains[g],e.getKey()),e.getValue(),"해당 없음",e.getKey(),warningBasis[g]);
                sh.getRow(line++).getCell(2).setCellStyle(st.number);
            }
            if(g==4&&!s.rights().isEmpty()){
                row(sh,++line,st.header,"권리요청 유형","현재 상태","건수","","유형·상태 코드","집계 기준");
                for(var r:s.rights()){
                    String[] p=r.status().split(":",2);
                    row(sh,++line,st.body,p.length==2?StatusLabels.label("rights-type",p[0]):"유형 미분류",
                        StatusLabels.label("rights",p[p.length-1]),r.count(),"",r.status(),basis[g]);
                    sh.getRow(line).getCell(2).setCellStyle(st.number);
                }
                line++;
            }
            line+=2;
        }
        book.setPrintArea(0,0,5,0,sh.getLastRowNum());
    }
    private static Map<String,Long> counts(String codes,List<StatusCount> rows,boolean rights){
        Map<String,Long> map=new LinkedHashMap<>();
        if(!codes.isBlank())for(String code:codes.split(" "))map.put(code,0L);
        for(var r:rows){String code=r.status();if(rights&&code.contains(":"))code=code.substring(code.indexOf(':')+1);map.merge(code,r.count(),Long::sum);}return map;
    }
    public static Row row(Sheet sh,int line,CellStyle style,Object... values){
        if(line>=1_048_576)throw new IllegalArgumentException("Excel 시트의 최대 행 수를 초과했습니다. 조회 기간을 줄여 주세요.");
        Row row=sh.createRow(line);row.setHeightInPoints(34);
        for(int i=0;i<values.length;i++){Cell c=row.createCell(i);c.setCellStyle(style);value(c,values[i]);}return row;
    }
    public static void value(Cell c,Object value){
        if(value==null)c.setBlank();
        else if(value instanceof Number n)c.setCellValue(n.doubleValue());
        else if(value instanceof LocalDate d)c.setCellValue(d.atStartOfDay());
        else if(value instanceof LocalDateTime d)c.setCellValue(d);
        else if(value instanceof java.sql.Timestamp t)c.setCellValue(t.toInstant().atZone(ZONE).toLocalDateTime());
        else if(value instanceof java.sql.Date d)c.setCellValue(d.toLocalDate().atStartOfDay());
        else if(value instanceof Boolean b)c.setCellValue(b?"예":"아니오");
        else {String text=value.toString();if(text.length()>32767)throw new IllegalArgumentException("Excel 셀의 최대 글자 수를 초과한 내역이 있습니다.");c.setCellValue(text);}
    }
}
