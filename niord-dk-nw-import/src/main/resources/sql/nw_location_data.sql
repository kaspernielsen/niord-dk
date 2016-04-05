 SELECT
  pt.ptnNo                 AS pointIndex,
  pt.latitude              AS pointLatitude,
  pt.longitude             AS pointLongitude,
  pt.radius                AS pointRadius
FROM
  message msg
  LEFT JOIN location loc            ON msg.locationId = loc.id
  LEFT JOIN point pt                ON loc.id = pt.locationId
WHERE
  msg.id = :id
ORDER BY
   pt.ptnNo