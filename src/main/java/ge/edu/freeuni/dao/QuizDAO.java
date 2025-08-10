package ge.edu.freeuni.dao;
import ge.edu.freeuni.model.QuizEngine.Question.*;
import ge.edu.freeuni.model.QuizEngine.Quiz;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.Access;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;


@Component("quizzes")
public class QuizDAO {

    @Autowired
    private BasicDataSource db;


    public QuizDAO() { }

    public void insertQuiz(Quiz quiz) throws SQLException {
        String sql = "INSERT INTO quizzes (name, description,num_questions,random_order, one_page, immediate_correction, practice_mode,creator_username) VALUES (?, ?, ?, ?, ?, ?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, quiz.getQuizName());
            stmt.setString(2, quiz.getDescription());
            stmt.setInt(3,quiz.getNQuestions());
            stmt.setBoolean(4, quiz.isRandomOrder());
            stmt.setBoolean(5, quiz.isOnePage());
            stmt.setBoolean(6, quiz.isImmediateCorrection());
            stmt.setBoolean(7, quiz.isPracticeMode());
            stmt.setString(8, quiz.getCreatorUsername());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()){
                    int quizId = rs.getInt(1);
                    insertQuestions(quizId, quiz.getQuestions());


                }else{
                    throw new SQLException("Failed to insert new quiz");
                }
            }
        }

    }

    private void insertQuestions(int quizId, List<Question> questions) throws SQLException {
        String sql = "INSERT INTO questions (quiz_id, question_text, question_type, possible_answers, correct_answer, imageURL, order_matters) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Question question : questions) {
                stmt.setInt(1, quizId);
                stmt.setString(2, question.getQuestion());
                stmt.setString(3, question.getQuestionType());

                String possibleAnswers = null;
                String correctAnswer = null;
                String imageURL = null;
                boolean orderMatters = false;

                if (question instanceof Question_Response || question instanceof Fill_In_The_Blank) {
                    correctAnswer = (String) question.getCorrectAnswer();
                } else if (question instanceof Multiple_Choice) {
                    Multiple_Choice mc = (Multiple_Choice) question;
                    possibleAnswers = String.join(",", mc.getPossibleAnswers());
                    correctAnswer = mc.getCorrectAnswer();
                } else if (question instanceof Picture_Response) {
                    Picture_Response pr = (Picture_Response) question;
                    imageURL = pr.getImageURL();
                    correctAnswer = pr.getCorrectAnswer();
                } else if (question instanceof Multi_Answer) {
                    Multi_Answer ma = (Multi_Answer) question;
                    correctAnswer = String.join(",", ma.getCorrectAnswer());
                    orderMatters = ma.orderMatters();
                } else if (question instanceof Multi_Choice_Multi_Answer) {
                    Multi_Choice_Multi_Answer mcm = (Multi_Choice_Multi_Answer) question;
                    possibleAnswers = String.join(",", mcm.getPossibleAnswers());
                    correctAnswer = String.join(",", mcm.getCorrectAnswer());
                } else if (question instanceof Matching) {
                    Matching match = (Matching) question;
                    Map<String, String> pairs = match.getCorrectAnswer();
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, String> entry : pairs.entrySet()) {
                        sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
                    }
                    correctAnswer = sb.toString();
                }

                stmt.setString(4, possibleAnswers);
                stmt.setString(5, correctAnswer);
                stmt.setString(6, imageURL);
                stmt.setBoolean(7, orderMatters);

                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    //Deletes a Quiz(Admin Functionality)
    public boolean removeQuiz(int quizId) {
        String sql1 = "DELETE FROM questions WHERE quiz_id = ?";
        String sql2 = "DELETE FROM quizzes WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt1 = conn.prepareStatement(sql1);
             PreparedStatement stmt2 = conn.prepareStatement(sql2)) {
            stmt1.setInt(1, quizId);
            stmt2.setInt(1, quizId);
            return stmt1.executeUpdate() == 1 && stmt2.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove quiz: #" + quizId, e);
        }
    }

    public List<Question> getQuestions(int quizId) throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE quiz_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quizId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String prompt = rs.getString("question_text");
                    String type = rs.getString("question_type");
                    String correct = rs.getString("correct_answer");
                    String choices = rs.getString("possible_answers");
                    String image = rs.getString("imageURL");
                    boolean orderMatters = rs.getBoolean("order_matters");

                    Question q;
                    switch (type) {
                        case "Question-Response":
                            q = new Question_Response(prompt, type, correct);
                            break;
                        case "Fill in the Blank":
                            q = new Fill_In_The_Blank(prompt, type, correct);
                            break;
                        case "Multiple Choice":
                            List<String> choiceList = Arrays.asList(choices.split(","));
                            q = new Multiple_Choice(prompt, type, choiceList, correct);
                            break;
                        case "Picture-Response":
                            q = new Picture_Response(prompt, type, image, correct);
                            break;
                        case "Multi-Answer":
                            List<String> correctAnswers = Arrays.asList(correct.split(","));
                            q = new Multi_Answer(prompt, type, orderMatters, correctAnswers);
                            break;
                        case "Multiple Choice with Multiple Answers":
                            List<String> multiChoices = Arrays.asList(choices.split(","));
                            List<String> multiCorrect = Arrays.asList(correct.split(","));
                            q = new Multi_Choice_Multi_Answer(prompt, type, multiChoices, multiCorrect);
                            break;
                        case "Matching":
                            HashMap<String,String> pairs = new HashMap<>();
                            String [] paired = correct.split(";");
                            for (String pair : paired) {
                                String[] keyValue = pair.split("=");
                                pairs.put(keyValue[0], keyValue[1]);
                            }

                            q = new Matching(prompt, type, pairs);
                            break;
                        default:
                            throw new SQLException("Unknown question type: " + type);
                    }

                    q.setQuestionID(id);
                    q.setQuizID(rs.getInt("quiz_id"));
                    questions.add(q);
                }
            }
        }
        return questions;
    }

    public Quiz getQuiz(int quizId) {
        String sql = "SELECT * FROM quizzes WHERE id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, quizId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Quiz quiz = new Quiz();


                    quiz.setQuizID(rs.getInt("id"));
                    quiz.setQuizName(rs.getString("name"));
                    quiz.setDescription(rs.getString("description"));
                    quiz.setNQuestions(rs.getInt("num_questions"));
                    quiz.setRandomOrder(rs.getBoolean("random_order"));
                    quiz.setOnePage(rs.getBoolean("one_page"));
                    quiz.setImmediateCorrection(rs.getBoolean("immediate_correction"));
                    quiz.setPracticeMode(rs.getBoolean("practice_mode"));
                    quiz.setQuestions(getQuestions(quizId));
                    return quiz;

                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to getQuiz quiz: " + quizId, e);
        }
        return null;
    }

    public List<Quiz> getAllQuizzes() {
        List<Quiz> quizzes = new ArrayList<>();
        String sql = "SELECT * FROM quizzes ORDER BY id DESC";

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Quiz quiz = new Quiz();
                int quizId = rs.getInt("id");
                quiz.setQuizID(quizId);
                quiz.setQuizName(rs.getString("name"));
                quiz.setDescription(rs.getString("description"));
                quiz.setNQuestions(rs.getInt("num_questions"));
                quiz.setRandomOrder(rs.getBoolean("random_order"));
                quiz.setOnePage(rs.getBoolean("one_page"));
                quiz.setImmediateCorrection(rs.getBoolean("immediate_correction"));
                quiz.setPracticeMode(rs.getBoolean("practice_mode"));
                quiz.setCreatorUsername(rs.getString("creator_username"));
                quiz.setQuestions(getQuestions(quizId));
                quizzes.add(quiz);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get all quizzes", e);
        }
        return quizzes;
    }

    public List<Quiz> getPopularQuizzes(int limit) {
        List<Quiz> quizzes = new ArrayList<>();
        String sql = "SELECT q.*, COUNT(qa.attempt_id) as attempt_count " +
                "FROM quizzes q " +
                "LEFT JOIN quiz_attempts qa ON q.id = qa.quiz_id " +
                "GROUP BY q.id " +
                "ORDER BY attempt_count DESC, q.id DESC " +
                "LIMIT ?";

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Quiz quiz = new Quiz();
                    quiz.setQuizID(rs.getInt("id"));
                    quiz.setQuizName(rs.getString("name"));
                    quiz.setDescription(rs.getString("description"));
                    quiz.setNQuestions(rs.getInt("num_questions"));
                    quiz.setRandomOrder(rs.getBoolean("random_order"));
                    quiz.setOnePage(rs.getBoolean("one_page"));
                    quiz.setImmediateCorrection(rs.getBoolean("immediate_correction"));
                    quiz.setPracticeMode(rs.getBoolean("practice_mode"));
                    quiz.setCreatorUsername(rs.getString("creator_username"));
                    quizzes.add(quiz);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get popular quizzes", e);
        }
        return quizzes;
    }
    // return the number of existing Quizzes
    public int numberOfQuizzes() {
        int count = 0;
        String query = "SELECT COUNT(*) FROM quizzes";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve number of quizzes" + e);
        }


        return count;
    }

    //Gets Only Those Quizzes that have a name alike the user Input
    public List<Quiz> getQuizzesWithName(String name) throws SQLException {
        List<Quiz> quizzes = new ArrayList<>();
        String query = "SELECT id, name, description, creator, created_at FROM quizzes WHERE name LIKE ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, "%" + name + "%"); // Wildcard for partial match

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Quiz quiz = new Quiz();
                    int quizId = rs.getInt("id");
                    quiz.setQuizID(quizId);
                    quiz.setQuizName(rs.getString("name"));
                    quiz.setDescription(rs.getString("description"));
                    quiz.setRandomOrder(rs.getBoolean("random_order"));
                    quiz.setOnePage(rs.getBoolean("one_page"));
                    quiz.setImmediateCorrection(rs.getBoolean("immediate_correction"));
                    quiz.setPracticeMode(rs.getBoolean("practice_mode"));
                    quiz.setQuestions(getQuestions(quizId));
                    quiz.setCreatorUsername(rs.getString("creator_username"));

                    quizzes.add(quiz);
                }
            }
        }
        return quizzes;
    }

}