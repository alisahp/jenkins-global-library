# DevOpsLeadsApi
Simple Leads Api for our DevOps Practicum

## Getting Started
This project was developed using .Net Core version `3.1.201`

To build the project, run the command: `dotnet build`

To run the project's tests, run the command: `dotnet test`

To run the full project, run the command `dotnet run --project DevOpsLeadsApi.Api/`

Note: Each of the above commands will restore tha projects package dependencies.

## Endpoints
| Path              | Method   | Request  |
| ----------------- | -------- | -------- |
| `/api/leads`      | `GET`    |          |
| `/api/leads/{id}` | `GET`    |          |
| `/api/leads`      | `POST`   | Lead Obj |
| `/api/leads/{id}` | `PUT`    | Lead Obj |
| `/api/leads/{id}` | `DELETE` |          |

