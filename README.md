
# trusts-individual-check

Endpoint is at `/trusts-individual-check/individual-check`

Accepts a valid `IdMatchRequest` as a JSON encoded body, and returns an `IdMatchResponse` or an `IdMatchError` as a JSON encoded body.

### Running

To run locally using the micro-service provided by the service manager:

```bash
sm2 --start TRUSTS_ALL
```

If you want to run your local copy, then stop the frontend ran by the service manager and run your local code by using the following (port number is 9780 but is defaulted to that in build.sbt).

```bash
sbt run
```

### Testing
To test the service locally run use the following script, this will run both the unit and integration tests as well as check for dependency updates and check the coverage of the tests.

```bash
./run_all_tests.sh
```

### Error Handling

All errors from API#1585 are intercepted and respective error codes are returned:

```
HTTP Not Found

{
 "errors": [
   "Dependent service indicated that no data can be found"
 ]
}
```

```
HTTP Internal Server Error

{
 "errors": [
   "IF is currently experiencing problems that require live service intervention"
 ]
}
```

```
HTTP Service Unavailable

{
 "errors": [
   "Dependent service is unavailable"
 ]
}
```


If a user reaches the maximum number of attempts (x), an `IdMatchError` is returned with a "Individual check - retry limit reached (x)" message:

```
HTTP Forbidden

{
 "errors": [
   "Individual check - retry limit reached (3)"
 ]
}
```

If the initial request is invalid, an `IdMatchError` is returned with a message "Could not validate the request".

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
