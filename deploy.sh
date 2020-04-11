#!/bin/bash

# Fail if any script returns a nonzero exit code
set -e

cat <<EOF
############################
# KirBot Deployment Script #
############################
EOF

if [[ $(git rev-parse --is-inside-work-tree 2>/dev/null) == "true" ]]; then
  echo "Git repository has been initialized"
else
  echo "Git repository was not found."
  exit 1
fi

branch=${1:-master}

echo "Deploying branch $branch"
# Fetch the branch and check it out
git fetch origin "$branch"

commit_hash=$(git rev-parse "refs/remotes/origin/$branch^{commit}")

echo "Branch to deploy: $branch ($commit_hash)"
echo "Are you sure you want to deploy this branch? If not hit Ctrl + C now!"
sleep 5
echo "Deploying..."

echo "Backing up database..."
docker-compose exec db mysqldump kirbot --user=kirbot --password=kirbot > .data/last_backup.sql

git reset --hard "$commit_hash"

echo "Updating KirBotPanel"
git submodule init
git submodule update

echo "Building KirBot docker images"
docker-compose build

echo "Updating running images"
docker-compose up -d

echo "Waiting 5 seconds to give updated containers a chance to start"
sleep 5

echo "Installing composer dependencies"
docker-compose exec web composer install --no-dev

echo "Running database migrations"
docker-compose exec web php artisan migrate --force

# Enter KirBotPanel directory

echo "Updating Panel"
cd KirBotPanel

echo "Running yarn"
yarn install

echo "Building dependencies"
yarn run prod
echo "Dashboard has been built successfully"

# Leave KirBotPanel directory
cd ..

echo "Updating documentation"
cd docs
LC_ALL=C.UTF-8 mkdocs build
cd ..

echo "SUCCESS! Panel has been deployed. $branch ($commit_hash)"