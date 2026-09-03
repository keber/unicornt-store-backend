-- Reference data only. No user accounts and no credentials are seeded here.
-- Every statement is idempotent: the file is safe to re-run.
--
-- Vocabulary note: the storefront calls the physical article a "product type"
-- (T-shirt / Mug / Poster) and the shop section a "category" (DevOps, IT Crowd,
-- ...). The source catalogue (frontend public/data/products.json) uses the
-- opposite labels -- its "category" is the article and its "subcategory" is the
-- section -- so its "category" maps to product_type here and its "subcategory"
-- maps to category. Image files live in frontend public/<image_base>{,-card,-thumb}.webp.

INSERT INTO roles (name) VALUES
    ('ROLE_USER'),
    ('ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;

INSERT INTO product_types (name, slug) VALUES
    ('T-shirt', 't-shirt'),
    ('Mug',     'mug'),
    ('Poster',  'poster')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, slug) VALUES
    ('PM',          'pm'),
    ('Cloud',       'cloud'),
    ('DevOps',      'devops'),
    ('Enigma',      'enigma'),
    ('General',     'general'),
    ('IT Crowd',    'it-crowd'),
    ('Linux',       'linux'),
    ('Personajes',  'personajes'),
    ('Programador', 'programador'),
    ('QA',          'qa')
ON CONFLICT (slug) DO NOTHING;

-- Catalogue: 49 T-shirts from the frontend source data. Explicit ids keep the
-- rows aligned with products.json and with the image folders. The identity
-- sequence is realigned afterwards so admin-created products continue from 50.
INSERT INTO products (id, name, product_type_id, category_id, price, description, image_base, is_active, stock)
SELECT v.id, v.name,
       (SELECT id FROM product_types WHERE slug = 't-shirt'),
       c.id, v.price, v.description, v.image_base, TRUE, 50
FROM (VALUES
    (1, 'Polera ''I Can Explain It To You''', 'pm', 13990, 'Frase favorita de todo Project Manager ante una estimación imposible. Si llevas esta polera en una reunión de planificación, todos sabrán quién eres.', 'assets/img/pm/i-can-explain-it-to-you'),
    (2, 'Polera ''Cloud Architect''', 'cloud', 14990, 'Para quienes diseñan arquitecturas en nubes que a veces se van. Diagramas, flechas y más flechas. Todo bajo control... en teoría.', 'assets/img/cloud/cloud-arquitect'),
    (3, 'Polera ''Breaking Prod''', 'devops', 13990, 'Basada en Breaking Bad, pero el producto que rompes es producción. Ideal para quien deployó un cambio de 2 líneas y tumbó todo el sistema.', 'assets/img/devops/breaking-prod'),
    (4, 'Polera ''CI/CD or Die Trying''', 'devops', 13990, 'Cuando tu pipeline de integración continua es tu razón de vivir. Si el build falla, la vida falla. 100% cultura DevOps.', 'assets/img/devops/cicd-or-die-trying'),
    (5, 'Polera ''DevOps Acronym''', 'devops', 12990, '¿Qué significa DevOps realmente? Esta polera lo explica en detalle. Cada letra tiene su propia historia, probablemente más dramática que la anterior.', 'assets/img/devops/devops-acronym'),
    (6, 'Polera ''I Broke Prod Again''', 'devops', 13990, 'No es la primera vez. Tampoco será la última. Lleva con orgullo la insignia del desarrollador que hizo deploy el viernes a las 5 PM.', 'assets/img/devops/i-broke-prod-again'),
    (7, 'Polera ''It Works In My Container''', 'devops', 13990, 'La evolución del clásico ''funciona en mi máquina''. Ahora con Docker. El contenedor funciona, el problema está en todos los demás.', 'assets/img/devops/it-works-in-my-container'),
    (8, 'Polera ''No Deploy on Fridays''', 'devops', 14990, 'La regla de oro del desarrollo de software. Quien deployó en viernes y pasó un buen fin de semana, que tire la primera piedra. (Spoiler: nadie.)', 'assets/img/devops/no-deploy-fridays'),
    (9, 'Polera ''Enigma Blueprint''', 'enigma', 15990, 'Plano técnico de la mítica máquina Enigma. El artefacto que cambió el curso de la Segunda Guerra Mundial y que Alan Turing logró descifrar. Historia pura.', 'assets/img/enigma/enigma-blue-print'),
    (10, 'Polera ''Enigma Machine''', 'enigma', 15990, 'Ilustración detallada del dispositivo de cifrado más famoso de la historia. Para quienes aman la criptografía, la historia y las máquinas complejas.', 'assets/img/enigma/enigma-machine'),
    (11, 'Polera ''Don Ramón: La Venganza''', 'general', 12990, 'El meme latinoamericano más noble de internet. Don Ramón en su máxima expresión. Para quienes crecieron con El Chavo y conocen el verdadero significado del clásico.', 'assets/img/general/don-ramon-venganza'),
    (12, 'Polera ''Don Ramón: La Venganza II''', 'general', 12990, 'Porque una edición no era suficiente. La secuela que nadie pidió pero todos necesitaban. Diseño alternativo del meme que nos une como región.', 'assets/img/general/don-ramon-venganza-2'),
    (13, 'Polera ''No Lloren Por Mí''', 'general', 12990, 'Para el momento en que cierras el IDE por última vez en el día. O cuando el código que escribiste funciona a la primera. Ambas situaciones merecen esta polera.', 'assets/img/general/no-lloren-por-mi'),
    (14, 'Polera ''Stonks''', 'general', 12990, 'El meme financiero por excelencia. Cuando tus acciones van ''stonks'' pero no sabes por qué. Perfecta para reuniones de economía amateur y grupos de WhatsApp.', 'assets/img/general/stonks'),
    (15, 'Polera ''This Is Fine''', 'general', 12990, 'El perrito más tranquilo del mundo mientras todo arde a su alrededor. El meme que resume perfectamente el día a día en tecnología. Todo está bien. Todo está bien.', 'assets/img/general/this-is-fine'),
    (16, 'Polera ''0118 999 881 999 119 725 3''', 'it-crowd', 14990, 'El nuevo número de emergencias de IT Crowd. En caso de incendio en la oficina, llame primero a este número, luego evacuarse. Solo los verdaderos fans lo reconocen.', 'assets/img/it-crowd/0118-999-881-999-119-725-3'),
    (17, 'Polera ''RTFM''', 'it-crowd', 12990, 'Read The F***ing Manual. El consejo más honesto que puede dar un técnico. Antes de abrir un ticket, antes de mandar un correo, antes de preguntar.', 'assets/img/it-crowd/rtfm'),
    (18, 'Polera ''Choose Your Weapon''', 'it-crowd', 13990, 'Vim vs Emacs. nano para los no iniciados. La guerra de editores que ha durado décadas y no tiene fin. ¿Cuál es tu arma de elección?', 'assets/img/it-crowd/choose-your-weapon'),
    (19, 'Polera ''I Don''t Work Here''', 'it-crowd', 13990, 'Para cuando te preguntan por el WiFi en el supermercado porque llevas una polera de colores similares al uniforme. La polera perfecta para salir sin ser identificado.', 'assets/img/it-crowd/i-dont-work-here'),
    (20, 'Polera ''I Hope This Email Finds You Well''', 'it-crowd', 13990, 'La frase con la que comienza el 87% de todos los correos corporativos de la historia de la humanidad. Lleva contigo la esencia del email culture.', 'assets/img/it-crowd/i-hope-this-email-finds-you-well'),
    (21, 'Polera ''I Read Your Email''', 'it-crowd', 13990, 'IT puede ver todo. Absolutamente todo. Esta polera es un recordatorio amistoso (y levemente intimidante) de que el equipo técnico sabe más de lo que crees.', 'assets/img/it-crowd/i-read-your-email'),
    (22, 'Polera ''I See Dumb People''', 'it-crowd', 13990, 'Para el técnico que atiende soporte nivel 1 y escucha las preguntas más inverosímiles. Versión IT Crowd del clásico de El Sexto Sentido.', 'assets/img/it-crowd/i-see-dumb-people'),
    (23, 'Polera ''Type Google Into Google''', 'it-crowd', 12990, 'Si escribes Google en Google puedes romper internet. El consejo de IT Crowd que ningún técnico olvidará jamás. Wear it wisely.', 'assets/img/it-crowd/type-google-into-google'),
    (24, 'Polera ''Meh''', 'it-crowd', 11990, 'La respuesta universal del técnico ante prácticamente cualquier situación. Reunión de kickoff, nueva metodología, otro reorg. Meh. Solo meh.', 'assets/img/it-crowd/meh'),
    (25, 'Polera ''Moss: Keep Calm''', 'it-crowd', 13990, 'Moss recomendando mantener la calma y apagar el fuego con el resto del fuego. La lógica IT Crowd aplicada a situaciones de emergencia cotidiana.', 'assets/img/it-crowd/moss-keep-calm'),
    (26, 'Polera ''¿Lo Apagaste y lo Volviste a Encender?''', 'it-crowd', 14990, 'La pregunta que soluciona el 80% de los problemas técnicos. Moss lo dijo primero, el mundo lo aprendió después. El diagnóstico más poderoso del universo IT.', 'assets/img/it-crowd/moss-turn-it-off'),
    (27, 'Polera ''Music I Like''', 'it-crowd', 12990, 'Referencia directa al estilo musical único de Moss en IT Crowd. Para quienes tienen gustos musicales difíciles de explicar en reuniones sociales.', 'assets/img/it-crowd/music-i-like'),
    (28, 'Polera ''Pixel Pirate Flag''', 'it-crowd', 13990, 'Bandera pirata en pixel art de 8 bits. Para los digitales rebeldes, los que navegan sin VPN por aguas desconocidas. RGB o muerte.', 'assets/img/it-crowd/pixel-pirate-flag'),
    (29, 'Polera ''Roy: People, What a Bunch of Bastards''', 'it-crowd', 14990, 'La frase que Roy Trenneman dijo en IT Crowd y que todo técnico de soporte ha pensado al menos una vez. La cita definitiva del introvertido en entorno corporativo.', 'assets/img/it-crowd/roy-people'),
    (30, 'Polera ''The Cake Is a Lie''', 'it-crowd', 12990, 'El mensaje más famoso de Portal y toda la cultura gamer. Una promesa de recompensa que nunca llega. Como el bonus de fin de año o el deploy sin bugs.', 'assets/img/it-crowd/the-cake-is-a-lie'),
    (31, 'Polera ''The Sun Is Trying to Kill Me''', 'it-crowd', 13990, 'Moss de IT Crowd expresando su relación con el exterior de manera perfectamente precisa. Para developers que prefieren la luz del monitor a la del sol.', 'assets/img/it-crowd/the-sun-is-trying-to-kill-me'),
    (32, 'Polera ''sudo rm -rf /''', 'linux', 14990, 'El comando más peligroso y temido de la línea de comandos. Solo con fines educativos. No ejecutar en sistemas de producción. Ni en los de desarrollo. Ni en nada.', 'assets/img/linux/sudo-rm-rf'),
    (33, 'Polera ''AC/DC: Tesla vs Edison''', 'personajes', 15990, 'La guerra de corrientes más épica de la historia, en formato de portada de disco de rock. Tesla con corriente alterna vs Edison con continua. ¿De qué lado estás?', 'assets/img/personajes/acdc-tesla-edison'),
    (34, 'Polera ''Alan Turing''', 'personajes', 15990, 'Homenaje al padre de la computación moderna y la inteligencia artificial. Matemático, criptógrafo, héroe de guerra. El hombre que descifró Enigma y definió nuestra era.', 'assets/img/personajes/alan-turing'),
    (35, 'Polera ''Chuck Norris Doesn''t Code''', 'personajes', 13990, 'Chuck Norris no necesita compilar. Sus programas se ejecutan antes de escribirse. Para quienes conocen todos los Chuck Norris facts del mundo del desarrollo.', 'assets/img/personajes/chuck-norris-doesnt-code'),
    (36, 'Polera ''Nikola Tesla''', 'personajes', 14990, 'El genio incomprendido de la corriente alterna. Inventor del motor de inducción, la bobina Tesla y el sueño de la electricidad gratuita para todos. Un verdadero nerd ahead of his time.', 'assets/img/personajes/tesla'),
    (37, 'Polera ''Chuck Norris Facts''', 'personajes', 13990, 'Todo lo que necesitas saber sobre Chuck Norris en una polera. Cuando Chuck Norris hace un pull request, el código se aprueba solo. Los tests pasan por miedo.', 'assets/img/personajes/chuck-norris-facts'),
    (38, 'Polera ''Turing Test''', 'personajes', 14990, '¿Puedes distinguir a una máquina de un humano? La pregunta que Alan Turing planteó en 1950 y que ChatGPT definitivamente ya resolvió, ¿o no?', 'assets/img/personajes/turing-test'),
    (39, 'Polera ''C: You Have No Class''', 'programador', 13990, 'El clásico chiste de programación orientada a objetos. C es poderoso, C es veloz, C no tiene clases. Y eso lo hace único. Para los puristas de la gestión manual de memoria.', 'assets/img/programador/c-you-have-no-class'),
    (40, 'Polera ''CSS Is Awesome''', 'programador', 12990, 'El meme de CSS más famoso del mundo: la caja que dice ''CSS is awesome'' pero el texto se desborda del contenedor. El overflow del universo.', 'assets/img/programador/css'),
    (41, 'Polera ''CTM Compilará Todo Mañana''', 'programador', 13990, 'El chiste más local de nuestra colección. Para el desarrollador chileno que siempre tiene una solución para mañana. Versión criolla del procrastination coding.', 'assets/img/programador/ctm-compilara-todo-manana'),
    (42, 'Polera ''False: It''s Funny Because It''s True''', 'programador', 12990, 'Para los amantes de la lógica booleana y el humor de programador. ''2 + 2 = 5 is true for large values of 2''. La ironía computacional en su máxima expresión.', 'assets/img/programador/false-its-funny'),
    (43, 'Polera ''I Don''t Always Test My Code''', 'programador', 13990, 'Basada en el meme de ''The Most Interesting Man in the World''. I don''t always test my code, but when I do, I do it in production. La verdad más incómoda del desarrollo.', 'assets/img/programador/i-dont-always-test-my-code'),
    (44, 'Polera ''I''m Just Here for the Pizza''', 'programador', 12990, 'La motivación más honesta para asistir a cualquier evento tech, meetup, hackathon o standup. Pizza > todo lo demás. La verdad que nadie dice en voz alta.', 'assets/img/programador/im-just-here-for-the-pizza'),
    (45, 'Polera ''Coffee + Problem = Programmer''', 'programador', 12990, 'La ecuación más precisa de la ciencia computacional. A programmer is just a machine that turns coffee into code. Diseño clásico para la ecuación fundamental.', 'assets/img/programador/problem-coffee-programmer'),
    (46, 'Polera ''Programming Is 10% Writing Code''', 'programador', 13990, 'Y 90% entendiendo por qué no funciona. La estadística más precisa de la carrera. Debugging, Stack Overflow, y preguntas existenciales incluidos en el porcentaje.', 'assets/img/programador/programming-is-10-percent'),
    (47, 'Polera ''This Meeting Could Have Been an Email''', 'programador', 14990, 'La frase que uno piensa en el 73% de todas las reuniones de la historia corporativa. Para quienes valoran el deep work sobre las sinergias cross-funcionales de stakeholders.', 'assets/img/programador/this-meeting'),
    (48, 'Polera ''Quality Assurance''', 'qa', 13990, 'Para los guardianes del software. Los que encuentran los bugs antes que los usuarios (y cuando no es así, los que más se acuerdan). Diseño clásico para el QA Engineer.', 'assets/img/qa/quality-assurance'),
    (49, 'Polera ''Quality Assurance Vol. 2''', 'qa', 13990, 'Edición especial para los QA más comprometidos. Porque un solo diseño no captura toda la pasión por encontrar ese bug imposible de reproducir en cualquier otro ambiente.', 'assets/img/qa/quality-assurance-2')
) AS v(id, name, category_slug, price, description, image_base)
JOIN categories c ON c.slug = v.category_slug
ON CONFLICT (id) DO NOTHING;

-- Realign the identity sequence so the next generated product id is 50.
SELECT setval(pg_get_serial_sequence('products', 'id'),
              (SELECT COALESCE(MAX(id), 0) FROM products) + 1,
              false);
