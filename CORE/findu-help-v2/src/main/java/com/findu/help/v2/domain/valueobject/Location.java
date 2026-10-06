package com.findu.help.v2.domain.valueobject;

public record Location(
    String country,
    String department,
    String city
) {
    public Location {
        country = country != null ? country.trim() : "";
        department = department != null ? department.trim() : "";
        city = city != null ? city.trim() : "";
    }
}
