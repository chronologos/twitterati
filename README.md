# twitterati

Automatically track your Twitter followers in Git, using GitHub Actions.

This GitHub Action runs every hour to take a snapshot of your current followers. This is committed to the repository. 
Viewing the repository history allows you to see how your followers have changed over time. Other basic stats are also tracked (e.g. mutuals, people you follow who don't follow you). The idea being that curating the people you follow to include more mutuals and fewer big accounts will generally make your Twitter life more engaging and fruitful :)

## Usage

1. [Create](https://developer.twitter.com/en/apps) a Twitter App.
2. Clone this repository.
3. In the GitHub repository secrets, add CONSUMERKEY="your key" and CONSUMERSECRET="your secret" from your Twitter App's Consumer API keys. 

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