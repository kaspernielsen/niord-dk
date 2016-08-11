SELECT
  fap.lat_deg       as lat_deg,
  fap.lat_min       as lat_min,
  fap.NS            as lat_dir,
  fap.long_deg      as lon_deg,
  fap.long_min      as lon_min,
  fap.EW            as lon_dir
FROM
  firing_area fa
  LEFT JOIN firing_area_position fap ON fap.firing_area_id = fa.id
WHERE
  fa.id = ?
ORDER BY
  fap.sort_order
