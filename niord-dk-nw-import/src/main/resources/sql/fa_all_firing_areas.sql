
SELECT
  fa.id             AS id,
  fa.active         AS active,
  c.country_english AS area1_en,
  c.country_danish  AS area1_da,
  ma.area_english   AS area2_en,
  ma.area_danish    AS area2_da,
  fa.name_eng       AS area3_en,
  fa.name_dk        AS area3_da
FROM
  firing_area fa
  LEFT JOIN main_area ma ON fa.main_area_id = ma.id
  LEFT JOIN country c ON ma.countryId = c.id
ORDER BY
  fa.id

