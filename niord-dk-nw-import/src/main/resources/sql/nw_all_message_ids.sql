SELECT
  m.id as id
FROM
  message m
  LEFT JOIN location loc ON m.locationId = loc.id
  LEFT JOIN locationtype loctp ON loc.locationTypeId = loctp.id
  LEFT JOIN main_area a ON loc.areaId = a.id
  LEFT JOIN country c ON a.countryId = c.id
WHERE
  c.abbreviation = 'DK'
  AND m.isLatest = 1
  AND m.draft = 0
  AND m.validFrom > ?
ORDER BY m.validFrom ASC
