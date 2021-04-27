package net.lelyak.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lelyak.model.Team;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nazar Lelyak.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

    private final EntityManager em;

    /*private final JdbcTemplate jdbcTemplate;*/

    @Override
    @Transactional
    public void afterJob(JobExecution jobExecution) {

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("!!! JOB FINISHED! Time to verify the results");

            /*jdbcTemplate.query("SELECT team1, team2, date FROM match",
                    (rs, row) -> "Team 1: " + rs.getString(1) + " Team 2: " + rs.getString(2) + " Date: " + rs.getString(3))
                    .forEach(System.out::println);*/

            Map<String, Team> teamNameAndTeamMap = new HashMap<>();

            em.createQuery("select m.team1, count(*) from Match m group by m.team1", Object[].class)
                    .getResultList()
                    .stream()
                    .map(fromDb -> new Team((String) fromDb[0], (long) fromDb[1]))
                    .forEach(team -> teamNameAndTeamMap.put(team.getTeamName(), team));

            em.createQuery("select m.team2, count(*) from Match m group by m.team2", Object[].class)
                    .getResultList()
                    .forEach(fromDb -> {
                        Team team = teamNameAndTeamMap.get((String) fromDb[0]);
                        team.setTotalMatches(team.getTotalMatches() + (long) fromDb[1]);
                    });

            em.createQuery("select m.matchWinner, count(*) from Match m group by m.matchWinner", Object[].class)
                    .getResultList()
                    .forEach(fromDb -> {
                        Team team = teamNameAndTeamMap.get((String) fromDb[0]);
                        if (team != null) {
                            team.setTotalWins((long) fromDb[1]);
                        }
                    });

            teamNameAndTeamMap.values().forEach(em::persist);
            teamNameAndTeamMap.values().forEach(System.out::println);
        }
    }
}
