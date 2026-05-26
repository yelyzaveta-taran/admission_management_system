BEGIN;

WITH pending_status AS (
    SELECT status_id
    FROM application_status
    WHERE name = 'PENDING'
), ordered_programs AS (
    SELECT
        program_id,
        ROW_NUMBER() OVER (ORDER BY program_id) AS rn
    FROM educational_program
), new_applications(first_name, last_name, phone, email, comment, program_rn, datetime) AS (
    VALUES
        ('Анастасія', 'Шевченко', '+380671248593', 'anastasiyashevchenko@gmail.com', 'Заявка подана через форму сайту.', 1, TIMESTAMPTZ '2026-05-20 09:14:00+03'),
        ('Олексій', 'Бондаренко', '+380931572846', 'oleksiybondarenko@gmail.com', 'Абітурієнт цікавиться деталями навчальної програми.', 2, TIMESTAMPTZ '2026-05-20 11:42:00+03'),
        ('Марина', 'Ткаченко', '+380501946327', 'marynatkachenko@gmail.com', 'Заявка на консультацію щодо старту навчання.', 3, TIMESTAMPTZ '2026-05-20 16:08:00+03'),
        ('Владислав', 'Кравченко', '+380967318254', 'vladyslavkravchenko@gmail.com', 'Потрібно уточнити зручний формат навчання.', 4, TIMESTAMPTZ '2026-05-21 08:55:00+03'),
        ('Софія', 'Романенко', '+380682547931', 'sofiyaromanenko@gmail.com', 'Заявка з публічної сторінки курсу.', 5, TIMESTAMPTZ '2026-05-21 10:27:00+03'),
        ('Дмитро', 'Савченко', '+380734169528', 'dmytrosavchenko@gmail.com', 'Користувач залишив коментар щодо попереднього досвіду.', 6, TIMESTAMPTZ '2026-05-21 14:36:00+03'),
        ('Катерина', 'Олійник', '+380995827143', 'katerynaoliynyk@gmail.com', 'Заявка очікує первинного опрацювання менеджером.', 7, TIMESTAMPTZ '2026-05-21 18:12:00+03'),
        ('Артем', 'Захарченко', '+380636415982', 'artemzakharchenko@gmail.com', 'Потрібно надати інформацію про графік занять.', 8, TIMESTAMPTZ '2026-05-22 09:03:00+03'),
        ('Вікторія', 'Даниленко', '+380973604218', 'viktoriyadanylenko@gmail.com', 'Абітурієнтка цікавиться умовами оплати.', 1, TIMESTAMPTZ '2026-05-22 12:19:00+03'),
        ('Максим', 'Гнатюк', '+380661739405', 'maksymhnatyuk@gmail.com', 'Заявка потребує перевірки контактних даних.', 2, TIMESTAMPTZ '2026-05-22 15:47:00+03'),
        ('Юлія', 'Ковтун', '+380505284716', 'yuliyakovtun@gmail.com', 'Заявка подана після перегляду опису курсу.', 3, TIMESTAMPTZ '2026-05-22 19:26:00+03'),
        ('Назар', 'Черненко', '+380938617245', 'nazarchernenko@gmail.com', 'Потрібно уточнити рівень підготовки.', 4, TIMESTAMPTZ '2026-05-23 08:41:00+03'),
        ('Олена', 'Мороз', '+380675932184', 'olenamoroz@gmail.com', 'Абітурієнтка обрала програму для початкового рівня.', 5, TIMESTAMPTZ '2026-05-23 10:58:00+03'),
        ('Роман', 'Іваненко', '+380731846259', 'romanivanenko@gmail.com', 'Заявка очікує першого контакту.', 6, TIMESTAMPTZ '2026-05-23 13:22:00+03'),
        ('Дарина', 'Лисенко', '+380969205713', 'darynalysenko@gmail.com', 'Потрібна консультація щодо навчального плану.', 7, TIMESTAMPTZ '2026-05-23 17:35:00+03'),
        ('Богдан', 'Клименко', '+380984713602', 'bohdanklymenko@gmail.com', 'Заявка з коментарем про бажаний вечірній графік.', 8, TIMESTAMPTZ '2026-05-24 09:18:00+03'),
        ('Поліна', 'Білик', '+380632915874', 'polinabilyk@gmail.com', 'Потрібно уточнити старт найближчої групи.', 1, TIMESTAMPTZ '2026-05-24 11:44:00+03'),
        ('Андрій', 'Сидоренко', '+380997462531', 'andriysydorenko@gmail.com', 'Абітурієнт цікавиться практичними проєктами.', 2, TIMESTAMPTZ '2026-05-24 14:09:00+03'),
        ('Христина', 'Мельничук', '+380665308197', 'khrystynamelnychuk@gmail.com', 'Заявка очікує призначення менеджеру.', 3, TIMESTAMPTZ '2026-05-24 18:51:00+03'),
        ('Михайло', 'Руденко', '+380507643928', 'mykhaylorudenko@gmail.com', 'Потрібно надати інформацію про формат навчання.', 4, TIMESTAMPTZ '2026-05-25 08:37:00+03'),
        ('Ірина', 'Поліщук', '+380934852617', 'irynapolishchuk@gmail.com', 'Заявка подана через форму реєстрації.', 5, TIMESTAMPTZ '2026-05-25 10:16:00+03'),
        ('Кирило', 'Федоренко', '+380971638452', 'kyrylofedorenko@gmail.com', 'Потрібно уточнити досвід програмування.', 6, TIMESTAMPTZ '2026-05-25 12:54:00+03'),
        ('Анна', 'Пономаренко', '+380682914763', 'annaponomarenko@gmail.com', 'Абітурієнтка зацікавлена у зміні спеціалізації.', 7, TIMESTAMPTZ '2026-05-25 16:33:00+03'),
        ('Павло', 'Гончарук', '+380735206941', 'pavlohoncharuk@gmail.com', 'Заявка очікує первинної комунікації.', 8, TIMESTAMPTZ '2026-05-25 20:07:00+03'),
        ('Злата', 'Семенюк', '+380963815274', 'zlatasemenyuk@gmail.com', 'Потрібна консультація щодо тривалості курсу.', 1, TIMESTAMPTZ '2026-05-26 09:05:00+03'),
        ('Ярослав', 'Хоменко', '+380503972641', 'yaroslavkhomenko@gmail.com', 'Заявка подана з мобільної версії сайту.', 2, TIMESTAMPTZ '2026-05-26 10:49:00+03'),
        ('Наталія', 'Кузьменко', '+380930641825', 'nataliyakuzmenko@gmail.com', 'Потрібно уточнити бажану дату старту.', 3, TIMESTAMPTZ '2026-05-26 13:28:00+03'),
        ('Остап', 'Шаповал', '+380975183406', 'ostapshapoval@gmail.com', 'Заявка очікує перевірки менеджером.', 4, TIMESTAMPTZ '2026-05-26 15:42:00+03')
)
INSERT INTO application (
    first_name,
    last_name,
    phone,
    email,
    comment,
    program_id,
    status_id,
    datetime
)
SELECT
    na.first_name,
    na.last_name,
    na.phone,
    na.email,
    na.comment,
    op.program_id,
    ps.status_id,
    na.datetime
FROM new_applications na
JOIN ordered_programs op ON op.rn = na.program_rn
CROSS JOIN pending_status ps
WHERE NOT EXISTS (
    SELECT 1
    FROM application a
    WHERE a.email = na.email
);

COMMIT;
