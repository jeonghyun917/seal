package kr.or.arte.seal.rights.persistence;

import kr.or.arte.seal.recipient.application.*;
import kr.or.arte.seal.rights.application.RightsRequestService;
import kr.or.arte.seal.rights.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named="SEAL_DB_PASSWORD",matches=".+")
class RightsRequestPostgresIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired RecipientManagementService recipients;@Autowired RightsRequestService rights;
 @org.junit.jupiter.api.BeforeEach void ensureApprover(){jdbc.update("INSERT INTO seal.admin_user(admin_user_id,login_id,display_name,organization_unit_id,password_hash,must_change_password) SELECT ?,'vheraclesv','통합테스트 승인자',organization_unit_id,password_hash,FALSE FROM seal.admin_user WHERE login_id='seal-admin' AND NOT EXISTS(SELECT 1 FROM seal.admin_user WHERE login_id='vheraclesv')",UUID.randomUUID());jdbc.update("INSERT INTO seal.user_scope(user_scope_id,admin_user_id,scope_type,scope_target_id,include_descendants,assigned_by) SELECT gen_random_uuid(),a.admin_user_id,'ORGANIZATION',a.organization_unit_id,TRUE,s.admin_user_id FROM seal.admin_user a JOIN seal.admin_user s ON s.login_id='seal-admin' WHERE a.login_id='vheraclesv' AND NOT EXISTS(SELECT 1 FROM seal.user_scope WHERE admin_user_id=a.admin_user_id AND scope_type='ORGANIZATION' AND scope_target_id=a.organization_unit_id)");}
 @Test void encryptsDetailAndPersistsTheDualControlLifecycle(){
  UUID org=id("SELECT organization_unit_id FROM seal.organization_unit WHERE root_unit=TRUE");
  List<UUID> actors=jdbc.query("SELECT admin_user_id FROM seal.admin_user WHERE account_status='ACTIVE' ORDER BY login_id LIMIT 2",(rs,n)->UUID.fromString(rs.getString(1)));
  assertThat(actors).hasSize(2);UUID creator=actors.get(0),decider=actors.get(1);
  UUID recipient=recipients.create(new CreateRecipientCommand(creator,org,"권리 요청 대상자","rights-"+UUID.randomUUID()+"@example.com","오프라인 권리요청 접수"));
  String plain="열람 범위와 처리 내역을 확인해 주세요.";LocalDate today=LocalDate.of(2026,8,24);
  UUID id=rights.create(org,recipient,null,RightsRequestType.ACCESS,today,today.plusDays(30),plain,creator);
  assertThat(rights.require(creator,id).detail()).isEqualTo(plain);
  assertThat(jdbc.queryForObject("SELECT position(convert_to(?,'UTF8') in detail_ciphertext)=0 FROM seal.rights_request WHERE rights_request_id=?",Boolean.class,plain,id)).isTrue();
  rights.verifyIdentity(creator,id,"OFFLINE_DOCUMENT","신분증 대면 확인");rights.startReview(creator,id);rights.decide(decider,id,true,"ACCESS_ALLOWED");rights.complete(creator,id,"자료 열람 제공 완료");
  assertThat(rights.require(creator,id).record().status()).isEqualTo(RightsRequestStatus.COMPLETED);
  assertThat(jdbc.queryForObject("SELECT count(*) FROM seal.rights_request_event WHERE rights_request_id=?",Integer.class,id)).isEqualTo(5);
 }
 private UUID id(String sql){return jdbc.queryForObject(sql,(rs,n)->UUID.fromString(rs.getString(1)));}
}
