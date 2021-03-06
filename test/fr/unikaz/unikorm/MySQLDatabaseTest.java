package fr.unikaz.unikorm;

import fr.unikaz.unikorm.api.Database;
import fr.unikaz.unikorm.connectors.sql.MySQLDatabase;
import fr.unikaz.unikorm.connectors.sql.filters.Filter;
import fr.unikaz.unikorm.connectors.sql.filters.Op;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySQLDatabaseTest {

    private MySQLDatabase database = new MySQLDatabase("orm_test", "root", "");
    private User et1 = new User("John Snow");


    void clearTable() {
        try {
            // some introspection to drop the table without changing the scope of `connection`
            Field connectionF = database.getClass().getDeclaredField("connection");
            connectionF.setAccessible(true);
            Connection connection = ((Connection) connectionF.get(database));
            String req = "drop table " + Database.getEntityName(User.class) + ";";
            connection.prepareStatement(req).execute();
            String req2 = "drop table " + Database.getEntityName(Message.class) + ";";
            connection.prepareStatement(req2).execute();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        } catch (SQLException ignored) {
            // not relevant as we delete table that probably not exists
        }
    }

    @Test
    void createTable() {
        clearTable();
        assertTrue(database.createTable(User.class));
        assertTrue(database.createTable(Message.class));
    }


    @Test
    void insert() throws Exception {
        database.createTable(User.class);
        assert database.insert(et1);
    }

    @Test
    void find() throws Exception {
        database.createTable(User.class);
        database.insert(et1);
        User et = database.find(User.class, new Filter(User.class, "id", Op.EQ, 1)).get(0);
        assertEquals(et.name, et1.name);
        String aValue = "An interesting value";
        // insert some more data
        for (int i = 0; i < 3; i++) {
            database.insert(new User(aValue));
        }
        for (int i = 0; i < 3; i++) {
            database.insert(new User(i + ""));
        }
        assert database.find(User.class, new Filter(User.class, "name", Op.EQ, aValue)).size() == 3;
        assert database.find(User.class, new Filter(User.class, "id", Op.EQ, 2)).get(0).name.equals(aValue);
        assert !database.find(User.class, new Filter(User.class, "id", Op.EQ, 5)).get(0).name.equals(aValue);
    }

    @Test
    void update() throws Exception {
        clearTable();
        database.createTable(User.class);
        String initialValue = "update: initial value";
        String updatedValue = "update: updated value and ' test";
        User e1 = new User(initialValue);
        database.insert(e1);
        e1 = database.find(User.class, new Filter(User.class, "name", Op.EQ, initialValue)).get(0);
        e1.name = updatedValue;
        database.update(e1);
        User e2 = database.find(User.class, new Filter(User.class, "id", Op.EQ, e1.id)).get(0);
        assert e1.id.equals(e2.id);
        assert e2.name.equals(updatedValue);
    }

    @Test
    void fetch() throws Exception {
        clearTable();
        database.createTable(User.class);
        database.createTable(Message.class);
        // test user
        User user = new User("John Snow");
        database.insert(user);
        database.fetch(user);
        assert user.id.equals(1);
        assert user.name.equals("John Snow");
        // test message
        Date date = Calendar.getInstance().getTime();
        Message message = new Message(user, "test message", date);
        database.insert(message);
        database.fetch(message);
        assert message.getMessage().equals("test message");
        assert message.getId().equals(1);
    }

    @Test
    void compositeTest() throws Exception {
        clearTable();
        database.createTable(User.class);
        database.createTable(Message.class);
        // create a test user
        User user = new User("John Snow");
        database.insert(user);
        user = database.find(User.class, new Filter(User.class, "name", Op.EQ, "John Snow")).get(0);
        assert user != null;
        // create a message
        Message message = new Message(user, "This is the message", Calendar.getInstance().getTime());
        assert database.insert(message);
        Message readMessage = database.find(Message.class, new Filter("user_id", Op.EQ, user.id)).get(0);
        assert readMessage.getMessage().equals(message.getMessage());
    }

    @Test
    void messageModelTest() throws Exception {
        //preparation
        clearTable();
        database.createTable(User.class);
        database.createTable(Message.class);
        database.insert(et1);
        User et2 = database.find(User.class, new Filter("name", Op.EQ, et1.name)).get(0);
        database.insert(new Message(et2, "Yop yop !", Calendar.getInstance().getTime()));
        //
        MessageModel messageModel = database.find(MessageModel.class, new Filter("user_id", Op.EQ, et2.id)).get(0);
        assert messageModel.userId == et2.id;
    }

    @Test
    void messageListModelTest() throws Exception {
        //preparation
        clearTable();
        database.createTable(User.class);
        database.createTable(Message.class);
        database.insert(et1);
        User et2 = database.find(User.class, new Filter("name", Op.EQ, et1.name)).get(0);
        database.insert(new Message(et2, "Yop yop 1 !", Calendar.getInstance().getTime()));
        database.insert(new Message(et2, "Yop yop 2 !", Calendar.getInstance().getTime()));
        database.insert(new Message(et2, "Yop yop 3 !", Calendar.getInstance().getTime()));
        //
        MessageListModel messageList = new MessageListModel();
        messageList.userId = et2.id;
        database.fetch(messageList);
        assert messageList.messages.size() == 3;
    }

    @Test
    void inTest() throws Exception{
        //preparation
        clearTable();
        database.createTable(User.class);
        database.insert(new User("Pierre"));
        database.insert(new User("Paul"));
        database.insert(new User("Jacques"));
        //
        List<User> u = database.find(User.class, new Filter("name", Op.IN, new String[]{"Pierre", "Paul"}));
        assert u.size() == 2;
    }
}