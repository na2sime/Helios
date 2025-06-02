package fr.nassime.helios.relation;

import fr.nassime.helios.annotation.Relation;
import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Field;

@Data
@Builder
public class RelationInfo {
    private RelationType type;

    private Field field;

    private Class<?> targetEntityClass;

    private String joinColumn;

    private String mappedBy;

    private String joinTable;

    private String inverseJoinColumn;

    private Relation.FetchType fetchType;


    private boolean cascade;

    private boolean orphanRemoval;


    private boolean collection;
}

