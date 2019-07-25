package org.jefrajames.mpql.preview.model;

import java.util.List;
import lombok.Data;
import org.eclipse.microprofile.graphql.FieldsOrder;
import org.eclipse.microprofile.graphql.InputField;
import org.eclipse.microprofile.graphql.InputFieldsOrder;
import org.eclipse.microprofile.graphql.InputType;
import org.eclipse.microprofile.graphql.Type;

@Type(value = "Team", description = "A team made of super heros")
@InputType(value = "TeamInput", description = "Input type for Team")
@FieldsOrder({"name", "members"})
@InputFieldsOrder({"members", "name"})
@Data
public class Team {

    @InputField(value = "name", description = "The name of the team")
    private String name;
    
    @InputField(value = "members", description = "The super heros who are part of the team")
    private List<SuperHero> members;

    public Team addMembers(SuperHero... heroes) {
        for (SuperHero hero : heroes) {
            members.add(hero);
        }
        return this;
    }

    public Team removeMembers(SuperHero... heroes) {
        for (SuperHero hero : heroes) {
            members.remove(hero);
        }
        return this;
    }
}
