package tests;

import models.Questionnaire;
import models.FeedbackResponse;
import services.FeedbackService;
import services.FeedbackResponseService;
import utils.DbConnection;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        FeedbackService qs = new FeedbackService(DbConnection.getInstance().getCnx());
        FeedbackResponseService fs = new FeedbackResponseService(DbConnection.getInstance().getCnx());

        try {
            System.out.println(qs.read());
            System.out.println(fs.read());
        } catch (SQLException e) {
            System.out.println("error" + e.getMessage());
        }
    }
}
