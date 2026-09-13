package kr.or.arte.seal.statistics.application;

import kr.or.arte.seal.common.web.StatusLabels;
import kr.or.arte.seal.recipient.crypto.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.io.*;
import java.sql.*;
import java.time.*;
import java.util.*;

@Service
public class StatisticsExportService {
    private final StatisticsQueryService statistics;
    private final NamedParameterJdbcTemplate jdbc;
    private final PersonalDataCipher cipher;
    private final Clock clock;
    public StatisticsExportService(StatisticsQueryService statistics,NamedParameterJdbcTemplate jdbc,
                                   PersonalDataCipher cipher,Clock clock){
        this.statistics=statistics;this.jdbc=jdbc;this.cipher=cipher;this.clock=clock;
    }

    // One database snapshot keeps totals and detail rows consistent during the export.
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public void write(UUID actor,LocalDate from,LocalDate to,OutputStream output) throws IOException {
        var snapshot=statistics.query(actor,from,to);
        var at=LocalDateTime.ofInstant(clock.instant(),StatisticsWorkbook.ZONE);
        var source=new XSSFWorkbook();
        try {
            var styles=new StatisticsWorkbook.Styles(source);
            StatisticsWorkbook.summary(source,styles,snapshot,from,to,at);
            source.getCreationHelper().createFormulaEvaluator().evaluateAll();
            try(var book=new SXSSFWorkbook(source,100)){
                book.setCompressTempFiles(true);
                try {
                    Map<String,Sheet> sheets=new LinkedHashMap<>();
                    Map<String,Integer> lines=new HashMap<>();
                    for(var section:StatisticsExportSection.values()){
                        Sheet sh=sheets.get(section.sheet);
                        if(sh==null){
                            sh=book.createSheet(section.sheet);sheets.put(section.sheet,sh);
                            StatisticsWorkbook.heading(sh,styles,section.sheet+" 상세 내역",from,to,at);
                            lines.put(section.sheet,8);
                        }
                        int end=detail(sh,lines.get(section.sheet),section,styles,actor,from,to);
                        lines.put(section.sheet,end+3);
                    }
                    book.setActiveSheet(0);
                    book.write(output);
                } finally {book.dispose();}
            }
        } finally {source.close();}
    }

    private int detail(Sheet sh,int line,StatisticsExportSection section,StatisticsWorkbook.Styles styles,
                       UUID actor,LocalDate from,LocalDate to){
        StatisticsWorkbook.row(sh,line,styles.title,section.title);
        sh.addMergedRegion(new CellRangeAddress(line,line++,0,5));
        StatisticsWorkbook.row(sh,line,styles.body,"기간 기준",section.basis);
        sh.addMergedRegion(new CellRangeAddress(line,line++,1,5));
        List<String> keys=new ArrayList<>(List.of("request_code","template_name","organization_name",
            "masked_name","masked_email","affiliation_name","job_title","status","occurred_at"));
        List<String> labels=new ArrayList<>(List.of("동의 요청번호","동의서명","소유 조직",
            "수신자 이름 (마스킹)","수신자 이메일 (마스킹)","수신자 소속기관","수신자 직책","현재 상태·지표",section.basis));
        for(var c:section.columns){keys.add(c.key());labels.add(c.label());}
        keys.add("id");labels.add("내역 ID");
        final int header=line;
        StatisticsWorkbook.row(sh,line++,styles.header,labels.toArray());
        for(int i=0;i<labels.size();i++){
            int width=keys.get(i).equals("decisions")||keys.get(i).endsWith("reason")||keys.get(i).equals("reason_text")?60:
                keys.get(i).equals("id")||keys.get(i).endsWith("_id")||keys.get(i).contains("hash")||keys.get(i).contains("sha256")?40:27;
            sh.setColumnWidth(i,Math.max(sh.getColumnWidth(i),width*256));
        }
        int[] next={line};
        var params=new MapSqlParameterSource().addValue("actor",actor)
            .addValue("from",java.sql.Date.valueOf(from)).addValue("to",java.sql.Date.valueOf(to));
        jdbc.execute(section.sql(),params,(org.springframework.jdbc.core.PreparedStatementCallback<Void>) ps -> {
            ps.setFetchSize(500);ps.setQueryTimeout(120);
            try(ResultSet rs=ps.executeQuery()){
                ResultSetMetaData md=rs.getMetaData();
                while(rs.next()){
                    Map<String,Object> record=new HashMap<>();
                    for(int i=1;i<=md.getColumnCount();i++)record.put(md.getColumnLabel(i),rs.getObject(i));
                    mask(record);
                    record.put("status",StatusLabels.label(section.domain,(String)record.get("status")));
                    label(record,"rights_type","rights-type");label(record,"destruction_method","destruction");
                    label(record,"hold_reason_code","hold-reason");
                    if("ALL".equals(record.get("withdrawal_scope")))record.put("withdrawal_scope","전체");
                    Object[] values=keys.stream().map(record::get).toArray();
                    Row row=StatisticsWorkbook.row(sh,next[0],next[0]%2==0?styles.body:styles.stripe,values);
                    row.setHeightInPoints(48);
                    for(int col=0;col<values.length;col++){
                        if(values[col] instanceof Number)row.getCell(col).setCellStyle(styles.number);
                        if(values[col] instanceof java.sql.Timestamp)row.getCell(col).setCellStyle(styles.dateTime);
                        else if(values[col] instanceof java.sql.Date)row.getCell(col).setCellStyle(styles.date);
                    }
                    next[0]++;
                }
            }
            return null;
        });
        int recordCount=next[0]-line;
        if(recordCount==0)StatisticsWorkbook.row(sh,next[0]++,styles.body,"조회 기간에 해당하는 내역이 없습니다.");
        else if(header==10)sh.setAutoFilter(new CellRangeAddress(header,next[0]-1,0,labels.size()-1));
        StatisticsWorkbook.row(sh,next[0]++,styles.body,"내역 건수",recordCount);
        sh.createFreezePane(1,11);
        return next[0];
    }

    private void mask(Map<String,Object> row){
        if("DESTROYED".equals(row.get("recipient_status"))){
            row.put("masked_name","파기 완료");row.put("masked_email","파기 완료");return;
        }
        UUID id=(UUID)row.get("recipient_id"), org=(UUID)row.get("organization_unit_id");
        String key=(String)row.get("encryption_key_id");
        String name=cipher.decrypt(new PersonalDataContext("RECIPIENT",id,org,"name"),
            new EncryptedValue(key,(byte[])row.get("recipient_name_ciphertext")));
        String email=cipher.decrypt(new PersonalDataContext("RECIPIENT",id,org,"email"),
            new EncryptedValue(key,(byte[])row.get("recipient_email_ciphertext")));
        row.put("masked_name",PersonalDataMasker.name(name));
        row.put("masked_email",PersonalDataMasker.email(email));
    }
    private static void label(Map<String,Object> row,String field,String domain){
        if(row.get(field)!=null)row.put(field,StatusLabels.label(domain,row.get(field).toString()));
    }
}
