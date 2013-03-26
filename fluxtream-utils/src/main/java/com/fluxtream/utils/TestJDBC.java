package com.fluxtream.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * User: candide
 * Date: 26/03/13
 * Time: 15:22
 */
public class TestJDBC {

    public static void main(String [] args) {
        try {
            System.out.println("Connection au driver JDBC");
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            System.out.println("Driver com.mysql.jdbc.Driver chargé");
            try {
                System.out.println("Connection a la base de données");
                Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3366/flx?user=root&password=fluxtream");
                System.out.println("Base de données connectée");
            }
            catch (SQLException ex) {
                // la connection a la base de données n'a pas pu etre établi
                // voici les codes erreurs retournés
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
            }
        }
        catch (Exception ex) {
            // Le driver n'a pas pu être chargé
            // vérifier que la variable CLASSPATH est bien renseignée
            System.out.println("Echec de chargement du driver");
        }
    }

}
