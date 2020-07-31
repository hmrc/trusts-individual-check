
# trusts-individual-check

Endpoint is at `/trusts-individual-check/individual-check`

Accepts a valid `IdMatchRequest` as a JSON encoded body, and returns an `IdMatchResponse` or an `IdMatchError` as a JSON encoded body.

### Error Handling

All errors from API#1585 are intercepted and an `IdMatchError` with a "Something went wrong" message.

If a user reaches the maximum number of attempts (x), an `IdMatchError` is returned with a "Individual check - retry limit reached (x)" message.

If the initial request is invalid, an `IdMatchError` is returned with a message "Could not validate the request".

### To-Do:

- Add authentication
- Update URL for API#1585

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
