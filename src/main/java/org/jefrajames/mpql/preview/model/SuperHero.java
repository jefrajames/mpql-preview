package org.jefrajames.mpql.preview.model;

import java.util.List;
import lombok.Data;
import org.eclipse.microprofile.graphql.FieldsOrder;
import org.eclipse.microprofile.graphql.InputField;
import org.eclipse.microprofile.graphql.InputFieldsOrder;
import org.eclipse.microprofile.graphql.InputType;
import org.eclipse.microprofile.graphql.Type;

@Type(value = "SuperHero", description = "A real super hero")
@FieldsOrder({"realName", "primaryLocation", "superPowers", "teamAffiliations", "name"})
@InputType(value = "SuperHeroInput", description = "Input type for SuperHero")
@InputFieldsOrder({"name", "realName", "primaryLocation", "superPowers", "teamAffiliations"})
@Data
public class SuperHero {

    @InputField(value = "teamAffiliations", description = "The teams the super hero is affiliated to")
    private List<Team> teamAffiliations;

    @InputField(value = "superPowers", description = "The super powers the super hero is supposed to have")
    private List<String> superPowers;

    @InputField(value = "primaryLocation", description = "Primary location of the super hero")
    private String primaryLocation;

    @InputField(value = "name", description = "Public name of the super hero")
    private String name;

    @InputField(value = "realName", description = "Real name of the super hero")
    String realName;

}
