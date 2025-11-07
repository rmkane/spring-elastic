package com.example.springelastic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "#{T(java.time.LocalDate).now().with(T(java.time.DayOfWeek).MONDAY).format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))}")
public class DocumentModel {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Text)
    private String fileName;
    
    @Field(type = FieldType.Text)
    private String content;
    
    @Field(type = FieldType.Long)
    private Long fileSize;
    
    @Field(type = FieldType.Text)
    private String contentType;
    
    @Field(type = FieldType.Date)
    private LocalDateTime uploadedAt;
}

