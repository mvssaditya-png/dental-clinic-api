package com.dentalclinic.treatment;

import com.dentalclinic.treatment.repository.TreatmentPlanRepository;
import com.dentalclinic.treatment.dto.TreatmentPlanSummary;
import org.junit.jupiter.api.Test;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Validates real JPQL and DTO constructors without a database or schema actions. */
class TreatmentPlanDiscoveryQueryTest {
 @Test void hibernateAcceptsBothProjectionsAndCountQueries() throws Exception {
  var registry=new StandardServiceRegistryBuilder()
      .applySetting("hibernate.dialect","org.hibernate.dialect.PostgreSQLDialect")
      .applySetting("hibernate.boot.allow_jdbc_metadata_access",false)
      .applySetting("hibernate.hbm2ddl.auto","none").build();
  try {
   var sources=new MetadataSources(registry);
   var scanner=new ClassPathScanningCandidateComponentProvider(false);
   scanner.addIncludeFilter(new AnnotationTypeFilter(jakarta.persistence.Entity.class));
   for(var bean:scanner.findCandidateComponents("com.dentalclinic"))sources.addAnnotatedClass(Class.forName(bean.getBeanClassName()));
   try(var factory=sources.buildMetadata().buildSessionFactory();var session=factory.openSession()){
    for(String name:java.util.List.of("discoverByPatient","discoverByCaseSheet")){
     var annotation=TreatmentPlanRepository.class.getMethod(name,UUID.class,UUID.class,Pageable.class).getAnnotation(org.springframework.data.jpa.repository.Query.class);
     var query=session.createQuery(annotation.value(),TreatmentPlanSummary.class);
     query.setParameter("parentId",UUID.randomUUID()).setParameter("clinicId",UUID.randomUUID()).setFirstResult(20).setMaxResults(20);
     assertEquals(20,query.getFirstResult());assertEquals(20,query.getMaxResults());
     assertNotNull(session.createQuery(annotation.countQuery(),Long.class));
    }
   }
  } finally {StandardServiceRegistryBuilder.destroy(registry);}
 }
}
