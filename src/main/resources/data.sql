INSERT INTO utilisateurs (username, password, nom, prenom, role, actif)
SELECT 'charlie.admin',
       '$2a$12$wxibz3CwqtXtIkLPwzMi4.msro7tVAIVBwVz2QQO878dxbha/Bmcy',
       'Charlie',
       'Angelo',
       'RESPONSABLE_PERSONNEL',
       true
WHERE NOT EXISTS (
  SELECT 1 FROM utilisateurs WHERE username = 'charlie.admin'
);