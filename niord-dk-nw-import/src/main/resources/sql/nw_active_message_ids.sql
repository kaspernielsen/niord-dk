SELECT
  m.id                AS id
FROM
  message m

--  The MSI admin use these criteria (where uc.abbreviation = 'DK') ... but we do not want access to the user table
--    LEFT JOIN user u ON m.usernameId = u.id
--    LEFT JOIN organisation o ON o.id = u.organisationId
--    LEFT JOIN country uc ON uc.id = o.countryId

  LEFT JOIN location loc ON m.locationId = loc.id
  LEFT JOIN locationtype loctp ON loc.locationTypeId = loctp.id
  LEFT JOIN main_area a ON loc.areaId = a.id
  LEFT JOIN country c ON a.countryId = c.id
WHERE
  c.abbreviation = 'DK'
  AND m.validFrom < now() + INTERVAL 1 DAY
  AND (m.validTo IS NULL OR m.validTo > now())
  AND m.isLatest = 1
  AND m.dateTimeDeleted IS NULL
  AND draft = 0
ORDER BY a.sortOrder, m.sortOrder, m.validFrom, id
