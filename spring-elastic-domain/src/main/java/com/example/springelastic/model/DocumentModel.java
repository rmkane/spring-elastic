package com.example.springelastic.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.ValueConverter;

import com.example.springelastic.convert.FlexibleInstantPropertyValueConverter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "#{T(java.time.LocalDate).now().with(T(java.time.DayOfWeek).MONDAY).format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))}")
public class DocumentModel {
    
    @Id
    private String id;
    
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)})
    private String fileName;
    
    @Field(type = FieldType.Text)
    private String content;
    
    @Field(type = FieldType.Long)
    private Long fileSize;
    
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)})
    private String contentType;
    
    @Field(
            type = FieldType.Date,
            format = {
                DateFormat.strict_date_optional_time,
                DateFormat.strict_date,
                DateFormat.epoch_millis
            })
    @ValueConverter(FlexibleInstantPropertyValueConverter.class)
    private Instant uploadedAt;
}

