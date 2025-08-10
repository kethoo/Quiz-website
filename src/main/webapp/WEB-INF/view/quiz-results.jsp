<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/QuizResultsStyle.css">
    <title>Quiz Results: ${quiz.quizName}</title>

    <style>

        .question-review {
            min-height: 200px;
            padding: 20px;
            margin-bottom: 25px;
        }


        .answer-comparison {
            min-height: 120px;
            line-height: 1.6;
            padding: 15px 0;
        }

        .user-answer, .correct-answer {
            min-height: 60px;
            margin-bottom: 20px;
            padding: 8px 0;
            word-wrap: break-word;
            overflow-wrap: break-word;
        }

        .question-review-header {
            margin-bottom: 20px;
            min-height: 30px;
        }

        .user-answer strong, .correct-answer strong {
            display: block;
            margin-bottom: 8px;
        }

        .user-answer div, .correct-answer div {
            display: block;
            margin-top: 8px;
            max-width: 100%;
            word-break: break-word;
            padding-left: 0;
        }
    </style>
</head>
<body>
<div class="panel">
    <div class="quiz-header">
        <div class="quiz-header-content">
            <h1>Quiz Completed!</h1>
            <h2>${quiz.quizName}</h2>
        </div>
    </div>

    <div class="content-area">
        <c:if test="${practiceMode}">
            <div class="practice-notice">
                This was a practice session - your score was not recorded
            </div>
        </c:if>

        <c:set var="scoreClass" value="" />
        <c:if test="${percentage >= 80}"><c:set var="scoreClass" value="" /></c:if>
        <c:if test="${percentage >= 60 && percentage < 80}"><c:set var="scoreClass" value="average" /></c:if>
        <c:if test="${percentage < 60}"><c:set var="scoreClass" value="poor" /></c:if>

        <div class="score-summary ${scoreClass}">
            <div class="score-summary-content">
                <div class="score-circle">
                    <div class="score-percentage">
                        <fmt:formatNumber value="${percentage}" maxFractionDigits="0"/>%
                    </div>
                </div>

                <div class="score-details">
                    <div class="score-item">
                        <h3>${score}/${totalQuestions}</h3>
                        <p>Questions Correct</p>
                    </div>
                    <div class="score-item">
                        <h3><fmt:formatNumber value="${percentage}" maxFractionDigits="1"/>%</h3>
                        <p>Final Score</p>
                    </div>
                    <div class="score-item">
                        <h3>${timeTaken}s</h3>
                        <p>Time Taken</p>
                    </div>
                </div>

                <div class="performance-message">
                    <c:choose>
                        <c:when test="${percentage >= 90}">
                            <h2>Excellent! Outstanding performance!</h2>
                        </c:when>
                        <c:when test="${percentage >= 80}">
                            <h2>Great job! Well done!</h2>
                        </c:when>
                        <c:when test="${percentage >= 70}">
                            <h2>Good work! Keep it up!</h2>
                        </c:when>
                        <c:when test="${percentage >= 60}">
                            <h2>Not bad! Room for improvement.</h2>
                        </c:when>
                        <c:otherwise>
                            <h2>Keep practicing! You'll get better!</h2>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <div class="nav-section">
            <a href="/quiz" class="btn btn-primary">Browse More Quizzes</a>
            <a href="/quiz/${quiz.quizID}" class="btn btn-secondary">Quiz Details</a>
            <a href="/history" class="btn btn-secondary">My History</a>
            <c:if test="${quiz.practiceMode}">
                <a href="/quiz/${quiz.quizID}/take?practiceMode=true" class="btn btn-success">Practice Again</a>
            </c:if>
            <c:if test="${not practiceMode}">
                <a href="/quiz/${quiz.quizID}/take" class="btn btn-success">Retake Quiz</a>
            </c:if>
        </div>

        <div class="questions-review">
            <h2>Question Review</h2>

            <c:forEach items="${questions}" var="question" varStatus="status">
                <c:set var="userAnswer" value="${userAnswers[question.questionID.toString()]}" />
                <c:set var="correctAnswers" value="${correctAnswersMap[question.questionID.toString()]}" />
                <c:set var="isCorrect" value="${gradingResults[question.questionID.toString()]}" />

                <div class="question-review ${isCorrect ? 'correct' : 'incorrect'}">
                    <div class="question-review-header">
                        Question ${status.index + 1}: ${question.question}
                        <span class="status-badge status-${isCorrect ? 'correct' : 'incorrect'}">
                                ${isCorrect ? 'Correct' : 'Incorrect'}
                        </span>
                    </div>

                    <div class="answer-comparison">
                        <div class="user-answer">
                            <strong>Your Answer:</strong>
                            <div class="${isCorrect ? 'correct-answer' : 'incorrect-answer'}">
                                <c:choose>
                                    <c:when test="${not empty userAnswer}">${userAnswer}</c:when>
                                    <c:otherwise><em>No answer provided</em></c:otherwise>
                                </c:choose>
                            </div>
                        </div>

                        <c:if test="${not isCorrect}">
                            <div class="correct-answer">
                                <strong>Correct Answer(s):</strong>
                                <div>${correctAnswers}</div>
                            </div>
                        </c:if>
                    </div>
                </div>
            </c:forEach>
        </div>

        <div style="text-align: center; margin-top: 40px; padding-top: 20px; border-top: 2px solid #ddd; color: #666;">
            <p style="font-size: x-small;"><em>Quiz Website - Person 3 Implementation</em></p>
        </div>
    </div>
</div>
</body>
</html>