/*
 * Copyright 2019 JF James.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jefrajames.mpql.preview.api;

import java.io.File;
import org.jefrajames.mpql.preview.client.GraphQLClient;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.logging.Level;
import javax.json.Json;
import javax.json.JsonPointer;
import javax.json.JsonValue;
import lombok.extern.java.Log;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jefrajames.mpql.preview.client.GraphQLException;
import org.jefrajames.mpql.preview.model.SuperHero;
import org.jefrajames.mpql.preview.model.Team;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This test is based on the SuperHeroDatabase.
 *
 * @author JF James
 */
@Log
@RunAsClient
public class HeroFinderTest extends Arquillian {

    private static final String[] SUPER_HERO_NAMES = {"Iron Man", "Spider Man", "Wolverine", "Starlord", "Captain America"};

    private static final String[] INITIAL_AVENGER_NAMES = {"Iron Man", "Spider Man", "Wolverine"};

    // Jsonb class is multi-threaded and recommended to be reused
    private static Jsonb jsonb;

    private static URL graphqlEndpoint;

    private static Properties CONFIG = new Properties();

    @BeforeClass
    public static void beforeClass() throws MalformedURLException, IOException {

        CONFIG.load(HeroFinderTest.class.getClassLoader().getResourceAsStream("graphql-config.properties"));
        graphqlEndpoint = new URL(CONFIG.getProperty("endpoint"));

        jsonb = JsonbBuilder.create();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (jsonb != null) {
            jsonb.close();
        }
    }

    @Deployment(name = "superhero")
    public static WebArchive createDeployment() {

        WebArchive archive = ShrinkWrap.create(WebArchive.class, "superhero.war");

        // Non-Java EE libraries
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve().withTransitivity().asFile();
        archive.addAsLibraries(libs);

        // Application packages excluding test classes
        archive.addPackages(true, Filters.exclude(".*Test.*"), "org.jefrajames.mpql.preview");

        // See how to exclude test files
        // Copied from src/test/resources
        archive.addAsWebInfResource("beans.xml");

        // Copied from src/main/resources
        archive.addAsResource("META-INF/services/javax.enterprise.inject.spi.Extension");
        archive.addAsResource("superheroes.json");

        log.log(Level.INFO, "deploying webarchive: {0}", archive.toString(true));

        return archive;
    }

    // Check that all names provided in the result are expected
    private boolean checkSuperHeroNames(List<SuperHero> superHeroes, String[] expectedNames) {

        int i = 0;
        for (; i < superHeroes.size(); i++) {
            int j = 0;
            for (; j < expectedNames.length; j++) {
                if (superHeroes.get(i).getName().equals(expectedNames[j])) {
                    break;
                }
            }
            if (j == expectedNames.length) {
                return false; // Name not expected
            }
        }

        return i == superHeroes.size(); // All names have been checked
    }

    @Test
    public void testQueryAllHeroes() {

        // No HTTP header here
        GraphQLClient client = new GraphQLClient(graphqlEndpoint);

        try {
            List<SuperHero> superHeroes = client.execute(CONFIG.getProperty("allHeroes"), (JsonObject jsonResponse) -> {
                JsonArray jsonSuperHeroes = jsonResponse.getJsonArray("allHeroes");
                // Thank you https://www.baeldung.com/java-json-binding-api for the magic syntax!
                return jsonb.fromJson(jsonSuperHeroes.toString(), new ArrayList<SuperHero>() {
                }.getClass().getGenericSuperclass());
            });

            assertTrue(checkSuperHeroNames(superHeroes, SUPER_HERO_NAMES));
        } catch (GraphQLException | IOException ex) {
            log.log(Level.SEVERE, "testQueryAllHeroes NOK: " + ex);
            fail();
        }

    }

    @Test
    public void testQueryAllAvengers() {

        // Example of HTTP httpHeaders (useless in this case, just for demo)
        Map<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put("Authentication", "Bearer: 123456");

        GraphQLClient client = new GraphQLClient(graphqlEndpoint, httpHeaders);

        try {
            List<SuperHero> superHeroes = client.execute(CONFIG.getProperty("allAvengers"), (JsonObject jsonResponse) -> {
                JsonArray jsonSuperHeroes = jsonResponse.getJsonArray("allHeroesInTeam");
                return jsonb.fromJson(jsonSuperHeroes.toString(), new ArrayList<SuperHero>() {
                }.getClass().getGenericSuperclass());
            });

            assertTrue(checkSuperHeroNames(superHeroes, SUPER_HERO_NAMES));
            assertTrue(superHeroes.size() >= INITIAL_AVENGER_NAMES.length);
        } catch (GraphQLException | IOException ex) {
            log.log(Level.SEVERE, "testQueryAllAvengers NOK: " + ex);
            fail();
        }
    }

    @Test
    public void testQueryAllAvengersWithVariable() {

        // Example of HTTP httpHeaders (useless in this case, just for demo)
        Map<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put("Authentication", "Bearer: 123456");

        GraphQLClient client = new GraphQLClient(graphqlEndpoint, httpHeaders);

        JsonObject variables = Json.createObjectBuilder().add("team", "Avengers").build();

        try {
            List<SuperHero> superHeroes = client.execute(CONFIG.getProperty("allAvengersWithVariables"), variables, (JsonObject jsonResponse) -> {
                JsonArray jsonSuperHeroes = jsonResponse.getJsonArray("allHeroesInTeam");
                return jsonb.fromJson(jsonSuperHeroes.toString(), new ArrayList<SuperHero>() {
                }.getClass().getGenericSuperclass());
            });

            assertTrue(checkSuperHeroNames(superHeroes, SUPER_HERO_NAMES));
        } catch (GraphQLException | IOException ex) {
            log.log(Level.SEVERE, "testQueryAllAvengersWithVariable NOK: " + ex);
            fail();
        }
    }

    @Test
    public void testMutationCreateNewHero() {

        GraphQLClient client = new GraphQLClient(graphqlEndpoint);

        try {
            SuperHero newHero = client.execute(CONFIG.getProperty("createNewHero"), (JsonObject jsonResponse) -> {
                JsonObject jsonNewHero = jsonResponse.getJsonObject("createNewHero");
                return jsonb.fromJson(jsonNewHero.toString(), SuperHero.class);
            });

            assertEquals(newHero.getName(), "Captain America");
            assertEquals(newHero.getRealName(), "Steven Rogers");
            assertEquals(newHero.getPrimaryLocation(), "New York, NY");
            assertTrue(newHero.getSuperPowers().size() == 2);

        } catch (GraphQLException | IOException ex) {
            log.log(Level.SEVERE, "testMutationCreateNewHero NOK: " + ex);
            fail();
        }
    }

    @Test
    public void testMutationCreateNewHeroWithVariables() {

        GraphQLClient client = new GraphQLClient(graphqlEndpoint);

        JsonObject variables = Json.createObjectBuilder()
                .add("name", "Captain America")
                .add("realName", "Steven Rogers")
                .add("primaryLocation", "New York, NY")
                .add("superPowers", Json.createArrayBuilder().add("Super strength").add("Vibranium Shield").build())
                .add("teamAffiliations", Json.createArrayBuilder().add(Json.createObjectBuilder().add("name", "Avengers").build()))
                .build();

        try {
            SuperHero newHero = client.execute(CONFIG.getProperty("createNewHeroWithVariables"), variables, (JsonObject jsonResponse) -> {
                JsonObject jsonNewHero = jsonResponse.getJsonObject("createNewHero");
                return jsonb.fromJson(jsonNewHero.toString(), SuperHero.class);
            });

            assertEquals(newHero.getName(), "Captain America");
            assertEquals(newHero.getRealName(), "Steven Rogers");
            assertEquals(newHero.getPrimaryLocation(), "New York, NY");
            assertTrue(newHero.getSuperPowers().size() == 2);

        } catch (GraphQLException | IOException ex) {
            log.log(Level.SEVERE, "testMutationCreateNewHeroWithVariables NOK: " + ex);
            fail();
        }
    }

    @Test
    public void testMutationAddHeroToTeam() {

        GraphQLClient client = new GraphQLClient(graphqlEndpoint);

        try {
            Team team = client.execute(CONFIG.getProperty("addHeroToTeam"), (JsonObject jsonResponse) -> {
                JsonObject jsonNewHero = jsonResponse.getJsonObject("addHeroToTeam");
                return jsonb.fromJson(jsonNewHero.toString(), Team.class);
            });

            assertEquals(team.getName(), "Avengers");
            assertTrue(team.getMembers().size() > INITIAL_AVENGER_NAMES.length);

        } catch (GraphQLException | IOException ex) {
            log.log(Level.SEVERE, "testMutationAddHeroToTeam NOK: " + ex);
            fail();
        }

    }

    @Test
    public void testMutationAddHeroToTeamWithVariables() {

        GraphQLClient client = new GraphQLClient(graphqlEndpoint);

        JsonObject variables = Json.createObjectBuilder()
                .add("heroName", "Starlord")
                .add("teamName", "Avengers")
                .build();

        try {
            Team team = client.execute(CONFIG.getProperty("addHeroToTeamWithVariables"), variables, (JsonObject jsonResponse) -> {
                JsonObject jsonNewHero = jsonResponse.getJsonObject("addHeroToTeam");
                return jsonb.fromJson(jsonNewHero.toString(), Team.class);
            });

            assertEquals(team.getName(), "Avengers");
            assertTrue(team.getMembers().size() > INITIAL_AVENGER_NAMES.length);

        } catch (GraphQLException | IOException ex) {
            log.log(Level.SEVERE, "testMutationAddHeroToTeamWithVariables NOK: " + ex);
            fail();
        }

    }

    public static class GraphQLType {
        public String name;
        public String kind;
    }

    private static boolean checkTeamInputType(List<GraphQLType> graphqlTypes) {
        for (int i = 0; i < graphqlTypes.size(); i++) {
            if (graphqlTypes.get(i).name.equals("TeamInput") && graphqlTypes.get(i).kind.equals("INPUT_OBJECT")) {
                return true;
            }
        }

        return false;
    }

    @Test
    public void checkExpectedTypes() {

        // No HTTP header here
        GraphQLClient client = new GraphQLClient(graphqlEndpoint);

        try {
            List<GraphQLType> graphqlTypes = client.execute(CONFIG.getProperty("allTypeNames"), (JsonObject jsonResponse) -> {
                JsonPointer pointer = Json.createPointer("/__schema/types");
                JsonValue value = pointer.getValue(jsonResponse);
                return jsonb.fromJson(value.toString(), new ArrayList<GraphQLType>() {
                }.getClass().getGenericSuperclass());
            });

            // graphqlTypes.forEach(t -> System.out.println(t.name + ", " + t.kind));

            assertTrue(checkTeamInputType(graphqlTypes));

        } catch (GraphQLException | IOException ex) {
            log.log(Level.SEVERE, "checkExpectedTypes NOK{0}", ex);
            fail();
        }

    }

}
