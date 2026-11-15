# IntelliJ workspace layout

Open the repository root, not `backend` or `frontend` alone.

```text
D-Z-Fashion-IntelliJ-Workspace/
├── .ai-workspace/                  # Private local prompts, skills, references, tracker; Git-ignored
├── backend/
│   ├── pom.xml                     # Maven reactor parent
│   ├── mvnw / mvnw.cmd
│   └── modules/
│       ├── api-gateway/
│       ├── identity-service/
│       ├── catalog-service/
│       ├── inventory-service/
│       ├── commerce-service/
│       ├── payment-service/
│       └── fulfillment-service/
├── frontend/                      # One Next.js application
├── scripts/dev/                   # Windows PowerShell env/run/check scripts
├── docs/                           # Tracked project/operator docs
├── .env                            # Local values; Git-ignored
├── .env.example                    # Safe variable-name template
├── .gitignore                      # Excludes private AI folder and secrets
└── README.md
```

Each backend business service will own its `config`, `controller`, `dto`, `entity`, `enums`, `exception`, `mapper`, `repository`, `security`, `service`, `validation` and migration folders where those concerns apply. The API Gateway is intentionally lightweight and should not own business entities.
