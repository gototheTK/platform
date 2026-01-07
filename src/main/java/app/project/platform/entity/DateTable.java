package app.project.platform.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@MappedSuperclass
@Getter
public class DateTable {

    @CreatedDate
    private CreatedDate createdDate;

    @LastModifiedDate
    private LastModifiedDate modifiedDate;

}
