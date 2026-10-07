package com.occupify.core.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillTest {

    @Test
    void testSkillBuilderAndGetters() {
        UUID id = UUID.randomUUID();
        Skill skill = Skill.builder()
                .name("Java")
                .build();
        skill.setId(id);

        assertEquals(id, skill.getId());
        assertEquals("Java", skill.getName());
    }
}
