package uz.programmer.courseBot.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uz.programmer.courseBot.model.Cart;

import java.util.List;
import java.util.Optional;

@Component
public class CartDAO {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CartDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
