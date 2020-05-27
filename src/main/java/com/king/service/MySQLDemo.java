package com.king.service;

import java.sql.*;

public class MySQLDemo {

    // MySQL 8.0 以下版本 - JDBC 驱动名及数据库 URL
    private final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private final String DB_URL = "jdbc:mysql://localhost:3306/ftp";

    // 数据库的用户名与密码，需要根据自己的设置
    private final String USER = "root";
    private final String PASS = "";

    public void showItems() {
        Connection conn = null;
        Statement stmt = null;
        try{
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);
            // 打开链接
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ftp?useSSL=false&serverTimezone=UTC",USER,PASS);
            // 执行查询
            //System.out.println(" 实例化Statement对象...");
            stmt = conn.createStatement();
            String sql = "SELECT * FROM client";
            ResultSet rs = stmt.executeQuery(sql);
            // 完成后关闭
            while(rs.next()) {
                String username = rs.getString("username");
                String password = rs.getString("password");
                System.out.println(String.format("username: %s password: %s", username, password));
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch(Exception se){
            // 处理 JDBC 错误
            se.printStackTrace();
        }// 处理 Class.forName 错误
        finally{
            // 关闭资源
            try{
                if(stmt!=null) stmt.close();
            }catch(SQLException ignored){
            }// 什么都不做
            try{
                if(conn!=null) conn.close();
            }catch(SQLException se){
                se.printStackTrace();
            }
        }

    }

    public boolean hasUsername(String username) {
        Connection conn = null;
        Statement stmt = null;
        try{
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);
            // 打开链接
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ftp?useSSL=false&serverTimezone=UTC",USER,PASS);
            // 执行查询
            //System.out.println(" 实例化Statement对象...");
            stmt = conn.createStatement();
            String sql = String.format("SELECT * FROM client WHERE username='%s'", username);
            ResultSet rs = stmt.executeQuery(sql);
            boolean flag = rs.next();
            // 完成后关闭
            rs.close();
            stmt.close();
            conn.close();
            return flag;
        } catch(Exception se){
            // 处理 JDBC 错误
            se.printStackTrace();
        }// 处理 Class.forName 错误
        finally{
            // 关闭资源
            try{
                if(stmt!=null) stmt.close();
            }catch(SQLException ignored){
            }// 什么都不做
            try{
                if(conn!=null) conn.close();
            }catch(SQLException se){
                se.printStackTrace();
            }
        }
        return false;
    }

    public boolean isValidPassword(String username, String password) {
        Connection conn = null;
        Statement stmt = null;
        try{
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);
            // 打开链接
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ftp?useSSL=false&serverTimezone=UTC",USER,PASS);
            // 执行查询
            //System.out.println(" 实例化Statement对象...");
            stmt = conn.createStatement();
            String sql = String.format("SELECT * FROM client WHERE username='%s' and password='%s'", username, password);
            ResultSet rs = stmt.executeQuery(sql);
            boolean flag = rs.next();
            // 完成后关闭
            rs.close();
            stmt.close();
            conn.close();
            return flag;
        } catch(Exception se){
            // 处理 JDBC 错误
            se.printStackTrace();
        }// 处理 Class.forName 错误
        finally{
            // 关闭资源
            try{
                if(stmt!=null) stmt.close();
            }catch(SQLException ignored){
            }// 什么都不做
            try{
                if(conn!=null) conn.close();
            }catch(SQLException se){
                se.printStackTrace();
            }
        }
        return false;
    }
}