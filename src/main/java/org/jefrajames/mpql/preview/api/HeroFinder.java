package org.jefrajames.mpql.preview.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.microprofile.graphql.Argument;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;

import org.jefrajames.mpql.preview.db.DuplicateSuperHeroException;
import org.jefrajames.mpql.preview.db.HeroDatabase;
import org.jefrajames.mpql.preview.db.UnknownTeamException;
import org.jefrajames.mpql.preview.model.SuperHero;
import org.jefrajames.mpql.preview.model.Team;
import javax.enterprise.context.RequestScoped;

@GraphQLApi
@RequestScoped
public class HeroFinder {
	@Inject
	private HeroDatabase heroDB;

	@Query(description = "Get all heroes")
	public Collection<SuperHero> allHeroes() {
		return heroDB.getAllHeroes();
	}
	
	@Query(description = "Get all heroes from a given city")
	public Collection<SuperHero> allHeroesIn(@Argument(value="city", description="The city to search for") String city) {
		return allHeroesByFilter(hero -> { return city.equals(hero.getPrimaryLocation());});
	}
	
	@Query(description = "Get all heroes with a given power")
	public Collection<SuperHero> allHeroesWithPower(@Argument(value="power", description="The power to search for") String power) {
		return allHeroesByFilter(hero -> { return hero.getSuperPowers().contains(power);});
	}

	@Query(description="Get all heroes from a given team")
	public Collection<SuperHero> allHeroesInTeam(@Argument(value="team", description="The team to search for") String teamName) throws UnknownTeamException {
		return heroDB.getTeam(teamName).getMembers();
	}

	@Query(description = "Get all hero teams")
	public Collection<Team> allTeams() {
		return heroDB.getAllTeams();
	}
	@Mutation(description = "Creates a new hero")
	public SuperHero createNewHero(@Argument(value="hero", description = "The hero to be created") SuperHero newHero) throws DuplicateSuperHeroException {
		heroDB.addHero(newHero);
		return heroDB.getHero(newHero.getName());
	}

	@Mutation(description="Adds a hero to the specified team and returns the updated team")
	public Team addHeroToTeam(@Argument(value="hero", description = "The hero name") String heroName, 
			         @Argument(value="team", description = "The team name to add to") String teamName)
							  throws UnknownTeamException {
		
		return heroDB.getTeam(teamName)
				     .addMembers( heroDB.getHero(heroName) );
	}

	@Mutation(description="Removes a hero to the specified team and returns the updated team")
	public Team removeHeroFromTeam(@Argument(value="hero", description="The hero name") String heroName, 
			              @Argument(value="team", description="The team to remove from") String teamName)
							       throws UnknownTeamException {
		
		return heroDB.getTeam(teamName)
				     .removeMembers( heroDB.getHero(heroName) );
	}

	private Collection<SuperHero> allHeroesByFilter(Predicate<SuperHero> predicate) {
		return heroDB.getAllHeroes()
                     .stream()
                     .filter(predicate)
                     .collect(Collectors.toCollection(ArrayList::new));
	}
}
