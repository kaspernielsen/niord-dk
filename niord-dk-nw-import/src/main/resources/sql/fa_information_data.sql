SELECT
  fa.name_eng       AS area3_en,
  fa.name_dk        AS area3_da,
  i.description_eng AS description_en,
  i.description_dk  AS description_da,
  i.info_type_id    AS info_type
FROM
  firing_area fa
  LEFT JOIN firing_area_information fai ON fai.firing_area_id = fa.id
  LEFT JOIN information i ON i.id = fai.information_id
WHERE
  fa.id = ?
ORDER BY
  i.info_type_id
