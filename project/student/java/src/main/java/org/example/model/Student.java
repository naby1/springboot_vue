package org.example.model;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Student {
    private int id;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 20, message = "姓名长度不能超过20个字符")
    private String name;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码必须为6-100位字符")
    private String passwd;

    @NotNull(message = "生日不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "性别不能为空")
    private Sex sex;

    @NotBlank(message = "电话不能为空")
    @Pattern(regexp = "^1+[3-8]+\\d{9}", message = "电话格式无效")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式无效")
    private String email;

    @NotBlank(message = "地址不能为空")
    @Size(max = 125, message = "地址长度不能超过125个字符")
    private String addr;
    private String education;

    @Column(name = "create_time", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime create_time;

    @Column(name = "last_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime last_time;


    public enum Sex {
        男, 女
    }

    public enum Education {
        博士, 硕士, 本科, 大专, 高中, 初中, 小学, 其他
    }
}