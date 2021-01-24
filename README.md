# twitterati

Automatically track your Twitter followers in Git, using GitHub Actions.

This GitHub Action runs every hour to take a snapshot of your current followers. This is committed to the repository. 
Viewing the repository history allows you to see how your followers have changed over time. Other basic stats are also tracked (e.g. mutuals, people you follow who don't follow you). The idea being that curating the people you follow to include more mutuals and fewer big accounts will generally make your Twitter life more engaging and fruitful :)

## Instructions

1. [Create](https://developer.twitter.com/en/apps) a Twitter App (so that we can call the Twitter API).
2. Create a new git repository (with a remote pointing to GitHub) and add this repository as a submodule of your repository.
   1. `mkdir twitter-tracker`
   2. `cd twitter-tracker`
   3. `git init`
   4. `git submodule add https://github.com/chronologos/twitterati`
   5. `git remote add origin https://github.com/${USERNAME}/${REMOTE_REPO}.git`
3. copy the .github folder from this repository into your repository. This contains the configuration for GitHub Actions.
   1. e.g. `cp -r twitterati/.github .`
   2. uncomment the yaml file
4. In your GitHub repository secrets, add the following vars: USERNAME="your twitter username", CONSUMERKEY="your key" and CONSUMERSECRET="your secret" from your Twitter App's Consumer API keys. 
5. Run by committing and pushing to origin.
6. If you wish to run this locally, you will need to set up a profiles.clj file (more instructions to come).

## License

Copyright © 2021 chronologos

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.