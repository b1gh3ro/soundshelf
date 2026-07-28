create table users (
    id            bigserial primary key,
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    display_name  varchar(100),
    created_at    timestamptz  not null default now()
);

create table library_items (
    id               bigserial primary key,
    user_id          bigint       not null references users (id) on delete cascade,
    apple_catalog_id bigint       not null,
    title            varchar(500) not null,
    artist_name      varchar(500) not null,
    genre            varchar(120),
    release_date     date,
    track_count      integer,
    artwork_url      varchar(1000),
    collection_price numeric(10, 2),
    user_rating      smallint,
    user_notes       text,
    created_at       timestamptz  not null default now(),
    updated_at       timestamptz  not null default now(),

    constraint uq_library_user_album unique (user_id, apple_catalog_id),
    constraint ck_library_rating check (user_rating is null or user_rating between 1 and 5)
);

-- Every library read is scoped to the owner, so user_id leads each index.
create index idx_library_user_created on library_items (user_id, created_at desc);
create index idx_library_user_genre on library_items (user_id, genre);
create index idx_library_user_release on library_items (user_id, release_date);
