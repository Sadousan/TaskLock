package com.example.tasklock.dao

import java.sql.Connection
import java.sql.DriverManager

object Database {
    private val banco : String = ""
    private val usuario : String = "mysql"
    private val senha : String = "JAreproVA"

    fun getConnection(): Connection{
        return DriverManager.getConnection("jdbc:mysql://$banco", "$usuario", "$senha")

    }
}