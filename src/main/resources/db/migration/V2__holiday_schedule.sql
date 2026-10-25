CREATE TABLE holidays (
    date DATE PRIMARY KEY,
    is_closed BOOLEAN DEFAULT FALSE NOT NULL,
    open_time TIME,
    close_time TIME
);

-- 1 января: бассейн полностью закрыт
INSERT INTO holidays (date, is_closed)
VALUES ('2027-01-01', TRUE);

-- 7 января: Рождество, сокращенный рабочий день с 10:00 до 18:00
INSERT INTO holidays (date, is_closed, open_time, close_time)
VALUES ('2027-01-07', FALSE, '10:00:00', '18:00:00');