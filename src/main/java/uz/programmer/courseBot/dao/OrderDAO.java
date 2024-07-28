package uz.programmer.courseBot.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uz.programmer.courseBot.model.Order;

import java.util.List;

@Component
public class OrderDAO {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public OrderDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Order> findTransactionsFromTo(long from, long to) {
        return jdbcTemplate.query("SELECT * FROM bot_order where create_time > ? and create_time < ? and cancel_time = 0", new Object[]{from, to}, new BeanPropertyRowMapper<>(Order.class));
    }
}
