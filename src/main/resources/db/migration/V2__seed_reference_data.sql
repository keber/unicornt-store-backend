-- Reference data only. No user accounts and no credentials are seeded here.

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
    ('Unicorns',  'unicorns'),
    ('Rainbows',  'rainbows'),
    ('Stars',     'stars')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO products (name, product_type_id, category_id, price, description, image_base, is_active, stock)
SELECT 'Classic Unicorn T-shirt',
       (SELECT id FROM product_types WHERE slug = 't-shirt'),
       (SELECT id FROM categories    WHERE slug = 'unicorns'),
       14990, 'Cotton T-shirt with the classic Unicornt print.', 'classic-unicorn-tshirt', TRUE, 25
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Classic Unicorn T-shirt');

INSERT INTO products (name, product_type_id, category_id, price, description, image_base, is_active, stock)
SELECT 'Rainbow Mug',
       (SELECT id FROM product_types WHERE slug = 'mug'),
       (SELECT id FROM categories    WHERE slug = 'rainbows'),
       7990, 'Ceramic mug that reveals a rainbow when filled with a hot drink.', 'rainbow-mug', TRUE, 40
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Rainbow Mug');

INSERT INTO products (name, product_type_id, category_id, price, description, image_base, is_active, stock)
SELECT 'Starry Night Poster',
       (SELECT id FROM product_types WHERE slug = 'poster'),
       (SELECT id FROM categories    WHERE slug = 'stars'),
       9990, 'A3 poster printed on matte paper.', 'starry-night-poster', TRUE, 15
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Starry Night Poster');
