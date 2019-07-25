package org.jefrajames.mpql.preview.db;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jefrajames.mpql.preview.model.SuperHero;
import org.jefrajames.mpql.preview.model.Team;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class HeroDatabase {

    Map<String, SuperHero> allHeroes = new HashMap<>();
    Map<String, Team> allTeams = new HashMap<>();

    @PostConstruct
    private void postConstruct() {
        
        try {
            Jsonb jsonb = JsonbBuilder.create();
            InputStream mapJson = getClass().getClassLoader()
                    .getResourceAsStream("/superheroes.json");
            addHeroes(jsonb.fromJson(mapJson,
                    new ArrayList<SuperHero>() {
                    }.getClass().getGenericSuperclass()));
        } catch (Exception ex) {
            log.log(Level.SEVERE, "HeroDatabase.postConstruct NOK {0}, is superheroes.json packaged?", ex.getMessage());
            throw new IllegalStateException(ex);
        }
        
        log.info("HeroDatabase initialized");
    }

    public SuperHero getHero(String name) {
        return allHeroes.get(name);
    }

    public Team getTeam(String name) throws UnknownTeamException {
        Team team = allTeams.get(name);
        if (team == null) {
            throw new UnknownTeamException(name);
        }
        return team;
    }

    public Collection<SuperHero> getAllHeroes() {
        return allHeroes.values();
    }

    public Collection<Team> getAllTeams() {
        return allTeams.values();
    }

    public int addHeroes(Collection<SuperHero> heroes) {
        int count = 0;
        for (SuperHero hero : heroes) {
            try {
                addHero(hero);
                count++;
            } catch (DuplicateSuperHeroException ex) {
                System.out.println("Already added : " + hero.getName());
            }
        }
        return count;
    }

    public void addHero(SuperHero hero) throws DuplicateSuperHeroException {
        allHeroes.put(hero.getName(), hero);
        List<Team> teams = hero.getTeamAffiliations();
        if (teams != null) {
            ListIterator<Team> iter = teams.listIterator();
            while (iter.hasNext()) {
                Team team = iter.next();
                Team existingTeam = allTeams.get(team.getName());
                if (existingTeam == null) {
                    existingTeam = new Team();
                    existingTeam.setName(team.getName());
                    allTeams.put(team.getName(), existingTeam);
                }
                iter.set(existingTeam);
                List<SuperHero> members = existingTeam.getMembers();
                if (members == null) {
                    members = new ArrayList<>();
                    existingTeam.setMembers(members);
                }
                members.add(hero);
            }
        }
    }
}
