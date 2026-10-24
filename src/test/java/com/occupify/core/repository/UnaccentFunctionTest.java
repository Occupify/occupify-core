package com.occupify.core.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UnaccentFunctionTest {

  @Autowired private EntityManager entityManager;

  @Test
  void testUnaccentFunction() {
    String result =
        (String)
            entityManager
                .createNativeQuery("SELECT unaccent('Lập trình viên')")
                .getSingleResult();
    assertEquals("Lap trinh vien", result);
  }
}
