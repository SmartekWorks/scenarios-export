### Build

`make.sh`

### Usage

`java -jar ScenariosExport.jar <path to config file> <path to target folder>`

### Config file

#### Config parameters

* `serverUrl`: the URL of SWATHub Server URL, such as http://www.swathub.com/
* `username`: the username of SWATHub Server
* `apiKey`: the api key for the user, same as the key for execution node
* `workspaceOwner`: the owner's username of the target workspace to export
* `workspaceName`: the name of the target workspace
* `setID`: the unique ID of the test set in the target workspace, which can be got from the test set url. For instance, the set ID is **9** in the url `http://swathub.com/app/support/samples/scenarios/set/9`

#### Sample config file

```
{
  "serverUrl": "http://swathub.com/",
  "username": "tester",
  "apiKey": "A7185B82DB6A4EFC9006",
  "workspaceOwner": "support",
  "workspaceName": "samples",
  "setID": "9"
}
```
