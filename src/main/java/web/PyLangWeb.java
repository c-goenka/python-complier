package web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "web", exclude = {
    org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
})
public class PyLangWeb {
    public static void main(String[] args) {
        SpringApplication.run(PyLangWeb.class, args);
    }
}