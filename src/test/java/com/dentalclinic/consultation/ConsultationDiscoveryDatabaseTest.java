package com.dentalclinic.consultation;

import com.dentalclinic.consultation.controller.PatientClinicalVisitsController;
import com.dentalclinic.consultation.service.ClinicalVisitService;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.user.entity.AppUser;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.generate_statistics=true"})
@EnabledIfEnvironmentVariable(named="CONSULTATION_DISCOVERY_TEST_DATABASE",matches="true")
@Transactional
class ConsultationDiscoveryDatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired com.dentalclinic.consultation.controller.ConsultationDiscoveryController controller;
    @Autowired com.dentalclinic.consultation.service.ConsultationDiscoveryService service;
    @Autowired EntityManagerFactory emf;
    @Autowired jakarta.persistence.EntityManager em;
    @org.springframework.test.context.DynamicPropertySource static void database(org.springframework.test.context.DynamicPropertyRegistry r){String url=System.getenv("SPRING_DATASOURCE_URL");if(url==null||!url.matches("jdbc:postgresql://localhost:5432/dental_clinic_v18_verification_10h2_[a-z0-9_]+"))throw new IllegalStateException("Isolated verification database required");r.add("spring.datasource.url",()->url);}
    UUID clinic, user, patient, doctor;
    AppUser actor;

    @BeforeEach void fixture() {
        assertTrue(jdbc.queryForObject("select current_database()",String.class)
                .startsWith("dental_clinic_v18_verification_10h2_"));
        clinic=UUID.randomUUID(); user=UUID.randomUUID(); patient=UUID.randomUUID(); doctor=UUID.randomUUID();
        jdbc.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,?)",clinic,clinic.toString(),"Visits test");
        jdbc.update("insert into app_user(id,clinic_id,first_name,last_name) values (?,?,?,?)",user,clinic,"Test","Doctor");
        jdbc.update("insert into patient(id,clinic_id,patient_number,first_name,gender,phone) values (?,?,?,'Patient','OTHER','123')",patient,clinic,patient.toString());
        jdbc.update("insert into doctor_profile(id,clinic_id,user_id) values (?,?,?)",doctor,clinic,user);
        actor=AppUser.builder().id(user).clinic(Clinic.builder().id(clinic).active(true).build()).build();
        login("CONSULTATION_VIEW","CASE_SHEET_VIEW");
    }
    void login(String... permissions) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,
                Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    UUID visit(String status, String sheetStatus, String start) {
        UUID appointment=UUID.randomUUID(), consultation=UUID.randomUUID();
        jdbc.update("insert into appointment(id,clinic_id,appointment_number,patient_id,doctor_id,appointment_date,start_time,end_time) values (?,?,?,?,?,current_date,'09:00','09:30')",appointment,clinic,appointment.toString(),patient,doctor);
        jdbc.update("insert into consultation(id,clinic_id,appointment_id,patient_id,doctor_id,status,started_at) values (?,?,?,?,?,?,cast(? as timestamp))",consultation,clinic,appointment,patient,doctor,status,start);
        if(sheetStatus!=null) {
            UUID sheet=UUID.randomUUID();
            jdbc.update("insert into case_sheet(id,clinic_id,consultation_id,appointment_id,patient_id,doctor_id,case_sheet_number,status,chief_complaint) values (?,?,?,?,?,?,?,?,?)",sheet,clinic,consultation,appointment,patient,doctor,sheet.toString(),sheetStatus,"Pain");
        }
        return consultation;
    }
    com.dentalclinic.consultation.dto.ConsultationPage list(String search,String status,int page,int size){return controller.list(null,page,size,search,status==null?null:com.dentalclinic.consultation.entity.ConsultationStatus.valueOf(status),null,null);}
    @Test void allStatusesSummaryAndEmpty(){assertEquals(0,list(null,null,0,20).totalElements());for(String status:List.of("STARTED","IN_PROGRESS","COMPLETED","CANCELLED"))visit(status,null,"2026-01-01 09:00");var r=list(null,null,0,20);assertEquals(4,r.totalElements());assertTrue(r.content().stream().allMatch(v->v.clinicId().equals(clinic)&&v.patientId().equals(patient)&&v.doctorName().equals("Test Doctor")));assertEquals(1,list(null,"COMPLETED",0,20).totalElements());}
    @Test void searchesNameNumberAppointmentAndConsultation(){UUID id=visit("STARTED",null,"2026-01-01 09:00");jdbc.update("update patient set first_name='Synthetic',last_name='Tester',patient_number='PAT-TARGET' where id=?",patient);jdbc.update("update appointment set appointment_number='APT-TARGET' where clinic_id=?",clinic);for(String q:List.of("  SYNTHETIC tester ","pat-target","apt-target",id.toString()))assertEquals(1,list(q,null,0,20).totalElements());assertEquals(0,list("absent",null,0,20).totalElements());}
    @Test void literalWildcards(){visit("STARTED",null,"2026-01-01 09:00");for(String q:List.of("%","_","!")){assertEquals(0,list(q,null,0,20).totalElements());jdbc.update("update patient set first_name=? where id=?","Literal"+q,patient);assertEquals(1,list(q,null,0,20).totalElements());jdbc.update("update patient set first_name='Patient' where id=?",patient);}}
    @Test void scopedCountsAndClinicOverride(){visit("STARTED",null,"2026-01-01 09:00");UUID other=UUID.randomUUID();jdbc.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,?)",other,other.toString(),"Other");assertEquals(1,controller.list(other,0,1,null,null,null,null).totalElements());actor=AppUser.builder().id(user).clinic(Clinic.builder().id(other).active(true).build()).build();login("CONSULTATION_VIEW");assertEquals(0,controller.list(clinic,0,1,null,null,null,null).totalElements());}
    @Test void platformAndInactiveClinic(){actor=AppUser.builder().id(user).build();login("CONSULTATION_VIEW");assertThrows(IllegalArgumentException.class,()->list(null,null,0,20));assertEquals(0,controller.list(clinic,0,20,null,null,null,null).totalElements());jdbc.update("update clinic set is_active=false where id=?",clinic);em.clear();emf.getCache().evictAll();assertThrows(AccessDeniedException.class,()->controller.list(clinic,0,20,null,null,null,null));}
    @Test void permissionRequired(){login("CASE_SHEET_VIEW");assertThrows(AccessDeniedException.class,()->list(null,null,0,20));login("CONSULTATION_VIEW");assertEquals(0,list(null,null,0,20).totalElements());}
    @Test void stablePagesAndTies(){List<UUID> ids=new ArrayList<>();for(int i=0;i<5;i++)ids.add(visit("STARTED",null,"2026-01-01 09:00"));ids.sort(Comparator.comparing(UUID::toString).reversed());assertEquals(ids.subList(0,2),list(null,null,0,2).content().stream().map(v->v.consultationId()).toList());assertEquals(ids.subList(2,4),list(null,null,1,2).content().stream().map(v->v.consultationId()).toList());assertEquals(3,list(null,null,0,2).totalPages());assertTrue(list(null,null,9,2).content().isEmpty());}
    @Test void nullStartsLast(){jdbc.execute("alter table consultation alter column started_at drop not null");UUID legacy=visit("STARTED",null,null),dated=visit("STARTED",null,"2026-01-01 09:00");assertEquals(List.of(dated,legacy),list(null,null,0,20).content().stream().map(v->v.consultationId()).toList());assertNull(list(null,null,0,20).content().get(1).startedAt());}
    @Test void appointmentDateBounds(){visit("STARTED",null,"2026-01-01 09:00");jdbc.update("update appointment set appointment_date='2026-02-02' where clinic_id=?",clinic);var date=java.time.LocalDate.of(2026,2,2);assertEquals(1,controller.list(null,0,20,null,null,date,date).totalElements());assertEquals(0,controller.list(null,0,20,null,null,date.plusDays(1),null).totalElements());assertThrows(IllegalArgumentException.class,()->controller.list(null,0,20,null,null,date,date.minusDays(1)));}
    @Test void invalidInputsAndLimits(){assertThrows(IllegalArgumentException.class,()->list(null,null,-1,20));assertThrows(IllegalArgumentException.class,()->list(null,null,0,0));assertThrows(IllegalArgumentException.class,()->list(null,null,Integer.MAX_VALUE,100));assertThrows(IllegalArgumentException.class,()->list("x".repeat(101),null,0,20));assertEquals(100,list("  ",null,0,1000).size());}
    @Test void boundedQueriesNoEntityCollectionsOrWrites(){for(int i=0;i<5;i++)visit("STARTED",null,"2026-01-01 09:00");var stats=emf.unwrap(SessionFactory.class).getStatistics();stats.clear();assertEquals(5,list(null,null,0,2).totalElements());assertTrue(stats.getPrepareStatementCount()<=3);assertEquals(0,stats.getEntityLoadCount());assertEquals(0,stats.getEntityUpdateCount());assertEquals(0,stats.getEntityInsertCount());}
    @Autowired com.dentalclinic.common.exception.GlobalExceptionHandler errors;
    @Test void httpContractAndInvalidEnums()throws Exception{var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(errors).build();mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/consultations")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.size").value(20));for(String query:List.of("?status=INVALID","?from=not-a-date","?clinicId=invalid","?page=-1"))mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/consultations"+query)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());}
}
