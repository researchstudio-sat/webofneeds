CREATE TABLE pushsubscriptions
(
  id bigint NOT NULL,
  auth character varying(255) NOT NULL,
  endpoint character varying(255) NOT NULL,
  key character varying(255) NOT NULL,
  updated timestamp without time zone NOT NULL,
  user_id bigint,
  CONSTRAINT pushsubscriptions_pkey PRIMARY KEY (id),
  CONSTRAINT fkabdxvemxlapjt0mkbme2r6h72 FOREIGN KEY (user_id)
      REFERENCES public.wonuser (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT uk3q280oqug9ms3deq019x9e19w UNIQUE (user_id, endpoint)
)
WITH (
  OIDS=FALSE
);