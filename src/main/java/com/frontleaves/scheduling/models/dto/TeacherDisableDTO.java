package com.frontleaves.scheduling.models.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TeacherDisableDTO  {
   private String teacherUuid;
   private Boolean status;
}
