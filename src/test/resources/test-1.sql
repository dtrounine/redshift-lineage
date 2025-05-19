SELECT
    herring,
    COALESCE(lobster, jackfish) AS lobster,
    COUNT(1) AS num,
    MAX(COUNT(1)) OVER (PARTITION BY eel) AS max_eel_num
INTO #temp_fishes
FROM atlantic.ocean ao
LEFT JOIN baltic.sea bs ON LOWER(ao.depth) = LOWER(bs.depth) AND ao.current = bs.current
LEFT JOIN arctic.ocean ON ao.depth = arctic.ocean.depth
WHERE 1=1
  AND NVL(lobster, eel) IS NOT NULL
GROUP BY
    herring,
    lobster
