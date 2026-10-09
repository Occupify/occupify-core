package com.occupify.core.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SkillTest {

  @Test
  void testSkillBuilderAndGetters() {
    UUID id = UUID.randomUUID();
    Skill skill = Skill.builder().name("Java").build();
    skill.setId(id);

    assertEquals(id, skill.getId());
    assertEquals("Java", skill.getName());
  }
}
